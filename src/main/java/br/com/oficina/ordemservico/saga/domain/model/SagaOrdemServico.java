package br.com.oficina.ordemservico.saga.domain.model;

import java.time.Duration;
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
 * <p>Implementacao incremental (Passo 1 da Fase 4): diagnostico, envio para orcamento e execucao
 * ainda sao acoes HTTP do proprio OS Service, registradas aqui; decisao e cancelamento do
 * orcamento ja chegam por mensagem.
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

    // Estado final quando as compensacoes terminarem (COMPENSADA ou ENCERRADA_SEM_EXECUCAO).
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
            case AGUARDANDO_EXECUCAO ->
                    new SagaOrdemServico(ordemDeServicoId, StatusSaga.AGUARDANDO_EXECUCAO, PassoSaga.EXECUCAO, agora);
            case SERVICO_EM_ANDAMENTO ->
                    new SagaOrdemServico(ordemDeServicoId, StatusSaga.EM_EXECUCAO, PassoSaga.EXECUCAO, agora);
            case OS_FINALIZADA, ENTREGUE -> new SagaOrdemServico(ordemDeServicoId,
                    motivo == MotivoEncerramento.SERVICO_CONCLUIDO || motivo == null
                            ? StatusSaga.CONCLUIDA : StatusSaga.ENCERRADA_SEM_EXECUCAO,
                    PassoSaga.FIM, agora);
        };
    }

    /** Orcamento gerado e enviado ao cliente: comeca a espera pela decisao, com prazo. */
    public void orcamentoGerado(UUID orcamentoId, LocalDateTime prazoDecisao, LocalDateTime agora) {
        exigir(StatusSaga.AGUARDANDO_DIAGNOSTICO, StatusSaga.GERANDO_ORCAMENTO);
        this.orcamentoId = orcamentoId;
        mudar(StatusSaga.AGUARDANDO_DECISAO, PassoSaga.DECISAO, agora);
        this.prazoExpiraEm = prazoDecisao;
    }

    /**
     * Cliente aprovou. Ate a reserva de pecas e o pagamento virarem passos da Saga, a ordem vai
     * direto para a fila de execucao.
     */
    public void orcamentoAprovado(UUID orcamentoId, LocalDateTime agora) {
        exigir(StatusSaga.AGUARDANDO_DECISAO);
        this.orcamentoId = orcamentoId;
        mudar(StatusSaga.AGUARDANDO_EXECUCAO, PassoSaga.EXECUCAO, agora);
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
        this.desfecho = StatusSaga.ENCERRADA_SEM_EXECUCAO;
        mudar(StatusSaga.COMPENSANDO, PassoSaga.COMPENSACAO, agora);
        definirPendentes(Set.of(MensagensSaga.ORCAMENTO_CANCELADO));
        this.tentativasPasso = 0;
        this.prazoExpiraEm = agora.plus(prazoResposta);
        enfileirarCompensacoesPendentes();
    }

    /** Confirmacao de uma compensacao. A ultima leva a Saga ao desfecho. */
    public void compensacaoConfirmada(String tipoEvento, LocalDateTime agora) {
        exigir(StatusSaga.COMPENSANDO);
        Set<String> pendentes = pendentes();
        if (!pendentes.remove(tipoEvento)) {
            throw new TransicaoSagaInvalidaException("Confirmacao " + tipoEvento + " nao era esperada");
        }
        definirPendentes(pendentes);
        if (pendentes.isEmpty()) {
            mudar(desfecho, PassoSaga.FIM, agora);
        } else {
            this.atualizadaEm = agora;
        }
    }

    /**
     * Prazo de resposta de uma compensacao vencido: reenvia (o participante e idempotente pelo
     * estado) ate {@code maxTentativas}; esgotou, a Saga para em {@link StatusSaga#FALHA_COMPENSACAO}.
     *
     * @return {@code true} se reenviou
     */
    public boolean respostaDaCompensacaoNaoChegou(LocalDateTime agora, Duration prazoResposta, int maxTentativas) {
        exigir(StatusSaga.COMPENSANDO);
        if (tentativasPasso >= maxTentativas) {
            mudar(StatusSaga.FALHA_COMPENSACAO, PassoSaga.FIM, agora);
            return false;
        }
        this.tentativasPasso++;
        this.prazoExpiraEm = agora.plus(prazoResposta);
        this.atualizadaEm = agora;
        enfileirarCompensacoesPendentes();
        return true;
    }

    public void execucaoIniciada(LocalDateTime agora) {
        exigir(StatusSaga.AGUARDANDO_EXECUCAO);
        mudar(StatusSaga.EM_EXECUCAO, PassoSaga.EXECUCAO, agora);
    }

    public void execucaoConcluida(LocalDateTime agora) {
        exigir(StatusSaga.EM_EXECUCAO);
        mudar(StatusSaga.CONCLUIDA, PassoSaga.FIM, agora);
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

    private void enfileirarCompensacoesPendentes() {
        for (String confirmacao : pendentes()) {
            if (MensagensSaga.ORCAMENTO_CANCELADO.equals(confirmacao)) {
                comandosAEnviar.add(new ComandoSaga(MensagensSaga.CANCELAR_ORCAMENTO, Filas.BILLING_COMMANDS,
                        new MensagensSaga.CancelarOrcamento(orcamentoId, motivo)));
            }
        }
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
