package br.com.oficina.ordemservico.saga.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.ordemservico.domain.model.MotivoEncerramento;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

/**
 * Estado da Saga de uma ordem de servico (ADR 0004). Uma Saga por OS.
 *
 * <p>O agregado decide as transicoes e os comandos a enviar ({@link #descarregarComandos()}); quem
 * grava no outbox, move a ordem e registra os passos e o orquestrador. Transicao fora do estado
 * esperado lanca {@link TransicaoSagaInvalidaException}.
 *
 * <p>Tres tipos de espera, todas com {@code prazoExpiraEm}:
 * <ul>
 *   <li><b>resposta a um comando de avanco</b> ({@code GerarOrcamento}, {@code ReservarPecas},
 *       {@code SolicitarPagamento}): vencido o prazo, o comando e reenviado ate o limite; esgotou, o
 *       passo e tratado como falha;</li>
 *   <li><b>confirmacoes de compensacao</b> ({@code COMPENSANDO} e {@code REVERTENDO_APROVACAO}): as que
 *       faltam ficam em {@code compensacoesPendentes}; esgotou, {@link StatusSaga#FALHA_COMPENSACAO};</li>
 *   <li><b>pessoas</b>: decisao do cliente e pagamento. Diagnostico e execucao nao tem prazo.</li>
 * </ul>
 */
@Entity
@Table(name = "saga_instancia")
public class SagaOrdemServico {

    private static final String SEPARADOR = ",";

    @Id
    private UUID id;

    @Column(name = "ordem_de_servico_id", nullable = false, unique = true)
    private UUID ordemDeServicoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSaga status;

    @Enumerated(EnumType.STRING)
    @Column(name = "passo_atual", nullable = false)
    private PassoSaga passoAtual;

    @Column(name = "orcamento_id")
    private UUID orcamentoId;

    @Column(name = "pagamento_id")
    private UUID pagamentoId;

    @Column(name = "link_pagamento", length = 1000)
    private String linkPagamento;

    // Estado ao qual a Saga vai quando as confirmacoes pendentes chegarem: COMPENSADA ou
    // ENCERRADA_SEM_EXECUCAO (em COMPENSANDO) e AGUARDANDO_DECISAO (em REVERTENDO_APROVACAO).
    @Enumerated(EnumType.STRING)
    private StatusSaga desfecho;

    private String motivo;

    // Confirmacoes de compensacao que faltam chegar (tipos de evento separados por virgula).
    @Column(name = "compensacoes_pendentes")
    private String compensacoesPendentes;

    @Column(name = "prazo_expira_em")
    private LocalDateTime prazoExpiraEm;

    @Column(name = "tentativas_passo", nullable = false)
    private int tentativasPasso;

    @Version
    private long versao;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    @Transient
    private final List<ComandoSaga> comandosAEnviar = new ArrayList<>();

    protected SagaOrdemServico() {
    }

    private SagaOrdemServico(UUID ordemDeServicoId, StatusSaga status, PassoSaga passo, LocalDateTime agora) {
        this.id = UUID.randomUUID();
        this.ordemDeServicoId = ordemDeServicoId;
        this.status = status;
        this.passoAtual = passo;
        this.criadaEm = agora;
        this.atualizadaEm = agora;
    }

    /** Saga nova, na abertura da OS. */
    public static SagaOrdemServico iniciar(UUID ordemDeServicoId, LocalDateTime agora) {
        return new SagaOrdemServico(ordemDeServicoId, StatusSaga.AGUARDANDO_DIAGNOSTICO, PassoSaga.DIAGNOSTICO, agora);
    }

    /**
     * Saga criada para uma OS que ja existia sem ela (aberta antes da Fase 4, ou por um caminho que
     * nao passou pela abertura). O estado vem do status atual da ordem - mesma regra do backfill da V23.
     */
    public static SagaOrdemServico adotar(
            UUID ordemDeServicoId, StatusOrdemDeServico statusOrdem, MotivoEncerramento motivo, LocalDateTime agora) {
        return switch (statusOrdem) {
            case OS_ABERTA, DIAGNOSTICO_EM_ANDAMENTO, DIAGNOSTICO_CONCLUIDO ->
                    new SagaOrdemServico(ordemDeServicoId, StatusSaga.AGUARDANDO_DIAGNOSTICO, PassoSaga.DIAGNOSTICO, agora);
            case ORCAMENTO_GERADO, AGUARDANDO_APROVACAO ->
                    new SagaOrdemServico(ordemDeServicoId, StatusSaga.AGUARDANDO_DECISAO, PassoSaga.DECISAO, agora);
            case AGUARDANDO_PAGAMENTO ->
                    new SagaOrdemServico(ordemDeServicoId, StatusSaga.AGUARDANDO_PAGAMENTO, PassoSaga.PAGAMENTO, agora);
            case AGUARDANDO_EXECUCAO ->
                    new SagaOrdemServico(ordemDeServicoId, StatusSaga.AGUARDANDO_EXECUCAO, PassoSaga.EXECUCAO, agora);
            case SERVICO_EM_ANDAMENTO ->
                    new SagaOrdemServico(ordemDeServicoId, StatusSaga.EM_EXECUCAO, PassoSaga.EXECUCAO, agora);
            case CANCELADA -> new SagaOrdemServico(ordemDeServicoId, StatusSaga.COMPENSADA, PassoSaga.FIM, agora);
            case OS_FINALIZADA, ENTREGUE -> new SagaOrdemServico(ordemDeServicoId,
                    motivo == MotivoEncerramento.SERVICO_CONCLUIDO || motivo == null
                            ? StatusSaga.CONCLUIDA : StatusSaga.ENCERRADA_SEM_EXECUCAO,
                    PassoSaga.FIM, agora);
        };
    }

    // ------------------------------------------------------------------ passo 2: orcamento

    /** Diagnostico concluido enviado para orcamento: comando {@code GerarOrcamento} ao Billing. */
    public void solicitarOrcamento(MensagensSaga.GerarOrcamento pedido, LocalDateTime agora, Duration prazoResposta) {
        exigir(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        this.motivo = null;
        mudar(StatusSaga.GERANDO_ORCAMENTO, PassoSaga.ORCAMENTO, agora);
        aguardarResposta(agora, prazoResposta);
        comandosAEnviar.add(new ComandoSaga(MensagensSaga.GERAR_ORCAMENTO, Filas.BILLING_COMMANDS, pedido));
    }

    /** Orcamento gerado e enviado ao cliente: comeca a espera pela decisao, com prazo. */
    public void orcamentoGerado(UUID orcamentoId, LocalDateTime prazoDecisao, LocalDateTime agora) {
        exigir(StatusSaga.GERANDO_ORCAMENTO);
        this.orcamentoId = orcamentoId;
        mudar(StatusSaga.AGUARDANDO_DECISAO, PassoSaga.DECISAO, agora);
        esperarAte(prazoDecisao);
    }

    /** O Billing nao conseguiu gerar o orcamento: a Saga volta a esperar o diagnostico. */
    public void orcamentoFalhou(String motivoFalha, LocalDateTime agora) {
        exigir(StatusSaga.GERANDO_ORCAMENTO);
        this.motivo = limitar(motivoFalha);
        mudar(StatusSaga.AGUARDANDO_DIAGNOSTICO, PassoSaga.DIAGNOSTICO, agora);
        this.prazoExpiraEm = null;
    }

    // ------------------------------------------------------------------ passo 3: decisao

    /** Cliente aprovou: comando {@code ReservarPecas} a Execution. A OS so muda com a reserva feita. */
    public void orcamentoAprovado(UUID orcamentoId, List<MensagensSaga.ItemPeca> itens, LocalDateTime agora, Duration prazoResposta) {
        exigir(StatusSaga.AGUARDANDO_DECISAO);
        this.orcamentoId = orcamentoId;
        this.motivo = null;
        mudar(StatusSaga.RESERVANDO_PECAS, PassoSaga.RESERVA, agora);
        aguardarResposta(agora, prazoResposta);
        comandosAEnviar.add(new ComandoSaga(MensagensSaga.RESERVAR_PECAS, Filas.EXECUTION_COMMANDS,
                new MensagensSaga.ReservarPecas(orcamentoId, List.copyOf(itens))));
    }

    /** Cliente rejeitou: encerramento de negocio, nao falha. */
    public void orcamentoRejeitado(UUID orcamentoId, LocalDateTime agora) {
        exigir(StatusSaga.AGUARDANDO_DECISAO);
        this.orcamentoId = orcamentoId;
        this.motivo = MotivoEncerramento.ORCAMENTO_RECUSADO.name();
        mudar(StatusSaga.ENCERRADA_SEM_EXECUCAO, PassoSaga.FIM, agora);
    }

    /**
     * Prazo de decisao vencido: o orcamento precisa ser cancelado no Billing. A Saga espera a
     * confirmacao ({@code OrcamentoCancelado}) para encerrar.
     */
    public void prazoDecisaoVencido(LocalDateTime agora, Duration prazoResposta) {
        exigir(StatusSaga.AGUARDANDO_DECISAO);
        this.motivo = MotivoEncerramento.ORCAMENTO_EXPIRADO.name();
        compensar(StatusSaga.COMPENSANDO, StatusSaga.ENCERRADA_SEM_EXECUCAO,
                Set.of(MensagensSaga.ORCAMENTO_CANCELADO), agora, prazoResposta);
    }

    // ------------------------------------------------------------------ passo 4: reserva

    /**
     * Pecas reservadas: comando {@code SolicitarPagamento} ao Billing, com o prazo de pagamento. Ate o
     * {@code PagamentoSolicitado} chegar, o prazo vigiado e o de resposta; depois, o do pagamento.
     */
    public void pecasReservadas(Instant expiraPagamento, LocalDateTime agora, Duration prazoResposta) {
        exigir(StatusSaga.RESERVANDO_PECAS);
        mudar(StatusSaga.AGUARDANDO_PAGAMENTO, PassoSaga.PAGAMENTO, agora);
        aguardarResposta(agora, prazoResposta);
        comandosAEnviar.add(new ComandoSaga(MensagensSaga.SOLICITAR_PAGAMENTO, Filas.BILLING_COMMANDS,
                new MensagensSaga.SolicitarPagamento(orcamentoId, expiraPagamento)));
    }

    /**
     * Estoque insuficiente: a aprovacao e revertida no Billing ({@code ReverterAprovacao}) e a Saga
     * volta a esperar a decisao do cliente quando a confirmacao chegar. Regra que ja existia em
     * AprovarOrcamentoService, agora como compensacao.
     */
    public void reservaFalhou(String motivoFalha, List<MensagensSaga.PecaFaltante> faltantes, LocalDateTime agora, Duration prazoResposta) {
        exigir(StatusSaga.RESERVANDO_PECAS);
        this.motivo = limitar(motivoFalha);
        compensar(StatusSaga.REVERTENDO_APROVACAO, StatusSaga.AGUARDANDO_DECISAO,
                Set.of(MensagensSaga.APROVACAO_REVERTIDA), agora, prazoResposta, faltantes);
    }

    /**
     * A Execution nao respondeu ao {@code ReservarPecas} nem aos reenvios: pode ter reservado ou nao.
     * Libera o que houver e reverte a aprovacao.
     */
    public void reservaSemResposta(LocalDateTime agora, Duration prazoResposta) {
        exigir(StatusSaga.RESERVANDO_PECAS);
        this.motivo = "Execution nao respondeu ao ReservarPecas";
        compensar(StatusSaga.REVERTENDO_APROVACAO, StatusSaga.AGUARDANDO_DECISAO,
                Set.of(MensagensSaga.APROVACAO_REVERTIDA, MensagensSaga.RESERVA_LIBERADA), agora, prazoResposta);
    }

    // ------------------------------------------------------------------ passo 5: pagamento

    /** Cobranca criada no Billing: guarda o link e passa a esperar o pagamento ate o prazo + margem. */
    public void pagamentoSolicitado(UUID pagamentoId, String link, LocalDateTime limite, LocalDateTime agora) {
        exigir(StatusSaga.AGUARDANDO_PAGAMENTO);
        this.pagamentoId = pagamentoId;
        this.linkPagamento = link;
        this.tentativasPasso = 0;
        this.atualizadaEm = agora;
        esperarAte(limite);
    }

    public boolean aguardandoCobranca() {
        return status == StatusSaga.AGUARDANDO_PAGAMENTO && pagamentoId == null;
    }

    /** Pagamento confirmado: a OS entra na fila de execucao. */
    public void pagamentoConfirmado(UUID pagamentoId, LocalDateTime agora) {
        exigir(StatusSaga.AGUARDANDO_PAGAMENTO);
        this.pagamentoId = pagamentoId;
        mudar(StatusSaga.AGUARDANDO_EXECUCAO, PassoSaga.EXECUCAO, agora);
    }

    /**
     * Pagamento recusado ou expirado: libera as pecas e cancela o orcamento. A OS e cancelada na hora;
     * a Saga termina {@code COMPENSADA} quando as duas confirmacoes chegarem.
     */
    public void pagamentoNaoConcluido(MotivoEncerramento motivoPagamento, LocalDateTime agora, Duration prazoResposta) {
        exigir(StatusSaga.AGUARDANDO_PAGAMENTO);
        this.motivo = motivoPagamento.name();
        compensar(StatusSaga.COMPENSANDO, StatusSaga.COMPENSADA,
                Set.of(MensagensSaga.RESERVA_LIBERADA, MensagensSaga.ORCAMENTO_CANCELADO), agora, prazoResposta);
    }

    // ------------------------------------------------------------------ passo 6: execucao

    public void execucaoIniciada(LocalDateTime agora) {
        exigir(StatusSaga.AGUARDANDO_EXECUCAO);
        mudar(StatusSaga.EM_EXECUCAO, PassoSaga.EXECUCAO, agora);
    }

    public void execucaoConcluida(LocalDateTime agora) {
        exigir(StatusSaga.EM_EXECUCAO);
        mudar(StatusSaga.CONCLUIDA, PassoSaga.FIM, agora);
    }

    /**
     * Mecanico abortou: estorna o pagamento e libera as pecas nao consumidas. Saga adotada de antes do
     * pagamento existir (sem {@code pagamentoId}) so libera as pecas.
     */
    public void execucaoFalhou(LocalDateTime agora, Duration prazoResposta) {
        exigir(StatusSaga.EM_EXECUCAO);
        this.motivo = MotivoEncerramento.FALHA_EXECUCAO.name();
        Set<String> confirmacoes = pagamentoId == null
                ? Set.of(MensagensSaga.RESERVA_LIBERADA)
                : Set.of(MensagensSaga.PAGAMENTO_ESTORNADO, MensagensSaga.RESERVA_LIBERADA);
        compensar(StatusSaga.COMPENSANDO, StatusSaga.COMPENSADA, confirmacoes, agora, prazoResposta);
    }

    // ------------------------------------------------------------------ compensacoes e prazos

    /**
     * Confirmacao de uma compensacao. A ultima leva a Saga ao desfecho; se o desfecho e voltar a
     * esperar a decisao (aprovacao revertida), comeca um prazo de decisao novo.
     */
    public void compensacaoConfirmada(String tipoEvento, LocalDateTime agora, LocalDateTime prazoDecisaoReaberta) {
        exigir(StatusSaga.COMPENSANDO, StatusSaga.REVERTENDO_APROVACAO);
        Set<String> pendentes = pendentes();
        if (!pendentes.remove(tipoEvento)) {
            throw new TransicaoSagaInvalidaException("Confirmacao " + tipoEvento + " nao era esperada");
        }
        definirPendentes(pendentes);
        if (!pendentes.isEmpty()) {
            this.atualizadaEm = agora;
            return;
        }
        StatusSaga destino = desfecho;
        this.desfecho = null;
        this.tentativasPasso = 0;
        if (destino == StatusSaga.AGUARDANDO_DECISAO) {
            mudar(StatusSaga.AGUARDANDO_DECISAO, PassoSaga.DECISAO, agora);
            esperarAte(prazoDecisaoReaberta);
        } else {
            this.desfecho = destino;
            mudar(destino, PassoSaga.FIM, agora);
        }
    }

    /**
     * O participante respondeu que a compensacao falhou ({@code EstornoFalhou}): continua pendente e
     * e reenviada quando {@code espera} passar - o reenvio conta como tentativa.
     */
    public void compensacaoFalhou(String confirmacaoEsperada, LocalDateTime agora, Duration espera) {
        exigir(StatusSaga.COMPENSANDO, StatusSaga.REVERTENDO_APROVACAO);
        if (!pendentes().contains(confirmacaoEsperada)) {
            throw new TransicaoSagaInvalidaException("Compensacao " + confirmacaoEsperada + " nao estava pendente");
        }
        this.atualizadaEm = agora;
        this.prazoExpiraEm = agora.plus(espera);
    }

    /**
     * Prazo de resposta de uma compensacao vencido: reenvia (o participante e idempotente pelo
     * estado) ate {@code maxTentativas}; esgotou, a Saga para em {@link StatusSaga#FALHA_COMPENSACAO}.
     *
     * @return {@code true} se reenviou
     */
    public boolean respostaDaCompensacaoNaoChegou(LocalDateTime agora, Duration prazoResposta, int maxTentativas) {
        exigir(StatusSaga.COMPENSANDO, StatusSaga.REVERTENDO_APROVACAO);
        if (tentativasPasso >= maxTentativas) {
            mudar(StatusSaga.FALHA_COMPENSACAO, PassoSaga.FIM, agora);
            return false;
        }
        this.tentativasPasso++;
        this.prazoExpiraEm = agora.plus(prazoResposta);
        this.atualizadaEm = agora;
        enfileirarCompensacoesPendentes(null);
        return true;
    }

    /**
     * Prazo de resposta de um comando de avanco vencido: reenvia o mesmo comando (messageId novo) ate
     * {@code maxTentativas}. Esgotou: devolve {@code false} e o orquestrador trata o passo como falha.
     */
    public boolean reenviarComandoDeAvanco(LocalDateTime agora, Duration prazoResposta, int maxTentativas) {
        ComandoSaga reenvio = switch (status) {
            case GERANDO_ORCAMENTO -> ComandoSaga.reenvio(MensagensSaga.GERAR_ORCAMENTO, Filas.BILLING_COMMANDS);
            case RESERVANDO_PECAS -> ComandoSaga.reenvio(MensagensSaga.RESERVAR_PECAS, Filas.EXECUTION_COMMANDS);
            case AGUARDANDO_PAGAMENTO -> {
                if (pagamentoId != null) {
                    throw new TransicaoSagaInvalidaException("Cobranca ja criada: nao ha comando aguardando resposta");
                }
                yield ComandoSaga.reenvio(MensagensSaga.SOLICITAR_PAGAMENTO, Filas.BILLING_COMMANDS);
            }
            default -> throw new TransicaoSagaInvalidaException("Saga em " + status + " nao aguarda resposta de comando de avanco");
        };
        if (tentativasPasso >= maxTentativas) {
            return false;
        }
        this.tentativasPasso++;
        this.prazoExpiraEm = agora.plus(prazoResposta);
        this.atualizadaEm = agora;
        comandosAEnviar.add(reenvio);
        return true;
    }

    /** Prazo que nao tem mais o que disparar (dado inconsistente): deixa de ser lido pelo agendador. */
    public void descartarPrazo(LocalDateTime agora) {
        this.prazoExpiraEm = null;
        this.atualizadaEm = agora;
    }

    public boolean prazoVencido(LocalDateTime agora) {
        return !status.isFinal() && prazoExpiraEm != null && prazoExpiraEm.isBefore(agora);
    }

    /** Comandos decididos desde a ultima chamada; a lista e esvaziada. */
    public List<ComandoSaga> descarregarComandos() {
        List<ComandoSaga> comandos = List.copyOf(comandosAEnviar);
        comandosAEnviar.clear();
        return comandos;
    }

    // ------------------------------------------------------------------ apoio

    private void compensar(StatusSaga estado, StatusSaga destino, Set<String> confirmacoes, LocalDateTime agora, Duration prazoResposta) {
        compensar(estado, destino, confirmacoes, agora, prazoResposta, null);
    }

    private void compensar(StatusSaga estado, StatusSaga destino, Set<String> confirmacoes, LocalDateTime agora,
            Duration prazoResposta, List<MensagensSaga.PecaFaltante> faltantes) {
        this.desfecho = destino;
        mudar(estado, estado == StatusSaga.REVERTENDO_APROVACAO ? PassoSaga.RESERVA : PassoSaga.COMPENSACAO, agora);
        definirPendentes(new LinkedHashSet<>(ordenadas(confirmacoes)));
        aguardarResposta(agora, prazoResposta);
        enfileirarCompensacoesPendentes(faltantes);
    }

    /** Ordem estavel dos comandos de compensacao (o Set.of nao garante ordem). */
    private static List<String> ordenadas(Set<String> confirmacoes) {
        return confirmacoes.stream().sorted().toList();
    }

    private void enfileirarCompensacoesPendentes(List<MensagensSaga.PecaFaltante> faltantes) {
        for (String confirmacao : pendentes()) {
            switch (confirmacao) {
                case MensagensSaga.ORCAMENTO_CANCELADO -> comandosAEnviar.add(new ComandoSaga(
                        MensagensSaga.CANCELAR_ORCAMENTO, Filas.BILLING_COMMANDS,
                        new MensagensSaga.CancelarOrcamento(orcamentoId, motivo)));
                case MensagensSaga.APROVACAO_REVERTIDA -> comandosAEnviar.add(new ComandoSaga(
                        MensagensSaga.REVERTER_APROVACAO, Filas.BILLING_COMMANDS,
                        new MensagensSaga.ReverterAprovacao(orcamentoId, motivo,
                                faltantes == null || faltantes.isEmpty() ? null : List.copyOf(faltantes))));
                case MensagensSaga.RESERVA_LIBERADA -> comandosAEnviar.add(new ComandoSaga(
                        MensagensSaga.LIBERAR_RESERVA, Filas.EXECUTION_COMMANDS,
                        new MensagensSaga.LiberarReserva(motivo)));
                case MensagensSaga.PAGAMENTO_ESTORNADO -> comandosAEnviar.add(new ComandoSaga(
                        MensagensSaga.ESTORNAR_PAGAMENTO, Filas.BILLING_COMMANDS,
                        new MensagensSaga.EstornarPagamento(orcamentoId, pagamentoId, motivo)));
                default -> throw new IllegalStateException("Confirmacao sem comando de compensacao: " + confirmacao);
            }
        }
    }

    private void aguardarResposta(LocalDateTime agora, Duration prazoResposta) {
        this.tentativasPasso = 0;
        this.prazoExpiraEm = agora.plus(prazoResposta);
    }

    private void esperarAte(LocalDateTime prazo) {
        this.tentativasPasso = 0;
        this.prazoExpiraEm = prazo;
    }

    private void exigir(StatusSaga... permitidos) {
        if (!Arrays.asList(permitidos).contains(status)) {
            throw new TransicaoSagaInvalidaException(
                    "Saga em " + status + "; esperado " + Arrays.toString(permitidos));
        }
    }

    private void mudar(StatusSaga novo, PassoSaga passo, LocalDateTime agora) {
        this.status = novo;
        this.passoAtual = passo;
        this.atualizadaEm = agora;
        if (novo.isFinal() || novo == StatusSaga.AGUARDANDO_EXECUCAO || novo == StatusSaga.EM_EXECUCAO) {
            this.prazoExpiraEm = null;
        }
    }

    private Set<String> pendentes() {
        Set<String> pendentes = new LinkedHashSet<>();
        if (compensacoesPendentes != null && !compensacoesPendentes.isBlank()) {
            pendentes.addAll(Arrays.asList(compensacoesPendentes.split(SEPARADOR)));
        }
        return pendentes;
    }

    private void definirPendentes(Set<String> pendentes) {
        this.compensacoesPendentes = pendentes.isEmpty() ? null : String.join(SEPARADOR, pendentes);
    }

    private static String limitar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= 255 ? texto : texto.substring(0, 252) + "...";
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrdemDeServicoId() {
        return ordemDeServicoId;
    }

    public StatusSaga getStatus() {
        return status;
    }

    public PassoSaga getPassoAtual() {
        return passoAtual;
    }

    public UUID getOrcamentoId() {
        return orcamentoId;
    }

    public UUID getPagamentoId() {
        return pagamentoId;
    }

    public String getLinkPagamento() {
        return linkPagamento;
    }

    public StatusSaga getDesfecho() {
        return desfecho;
    }

    public String getMotivo() {
        return motivo;
    }

    public Set<String> getCompensacoesPendentes() {
        return Set.copyOf(pendentes());
    }

    public LocalDateTime getPrazoExpiraEm() {
        return prazoExpiraEm;
    }

    public int getTentativasPasso() {
        return tentativasPasso;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getAtualizadaEm() {
        return atualizadaEm;
    }
}
