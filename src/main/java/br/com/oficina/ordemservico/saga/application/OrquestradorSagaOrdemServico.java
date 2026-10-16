package br.com.oficina.ordemservico.saga.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.domain.model.MotivoEncerramento;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.OrigemAlteracaoStatus;
import br.com.oficina.ordemservico.domain.model.PecaPrevistaOrdem;
import br.com.oficina.ordemservico.domain.model.ServicoOrdem;
import br.com.oficina.ordemservico.domain.model.SituacaoOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.ordemservico.saga.domain.model.ComandoSaga;
import br.com.oficina.ordemservico.saga.domain.model.MensagensSaga;
import br.com.oficina.ordemservico.saga.domain.model.PassoSaga;
import br.com.oficina.ordemservico.saga.domain.model.RegistroPassoSaga;
import br.com.oficina.ordemservico.saga.domain.model.RegistroPassoSaga.Tipo;
import br.com.oficina.ordemservico.saga.domain.model.SagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.model.StatusSaga;
import br.com.oficina.ordemservico.saga.domain.repository.SagaOrdemServicoRepository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Orquestrador da Saga da ordem de servico (ADR 0004).
 *
 * <p>Tres tipos de entrada:
 * <ul>
 *   <li><b>acoes registradas</b> - passos que ainda sao HTTP do proprio OS Service (envio para
 *       orcamento, inicio, conclusao e aborto da execucao): o servico ja moveu a ordem; aqui a Saga
 *       avanca e decide os comandos;</li>
 *   <li><b>eventos</b> dos participantes, por mensagem (orcamento, reserva de pecas, pagamento e as
 *       confirmacoes das compensacoes): aqui a Saga avanca, a ordem e movida com origem {@code SAGA}
 *       e os proximos comandos vao ao outbox;</li>
 *   <li><b>prazos</b> vencidos, pelo agendador: resposta a comandos (reenvio), decisao do cliente,
 *       pagamento e confirmacoes de compensacao.</li>
 * </ul>
 * Evento que nao cabe no estado atual e registrado como {@code EVENTO_IGNORADO} e confirmado - nunca
 * reprocessado em laco (ADR 0004, regra 3).
 */
@Service
public class OrquestradorSagaOrdemServico implements PassosDaSaga {
    private static final Logger log = LoggerFactory.getLogger(OrquestradorSagaOrdemServico.class);
    private static final ZoneId FUSO = ZoneId.systemDefault();

    private final SagaOrdemServicoRepository sagas;
    private final OrdemDeServicoRepository ordens;
    private final PublicadorMensagens publicador;
    private final FabricaEnvelopes fabrica;
    private final ApplicationEventPublisher eventos;
    private final JsonMapper jsonMapper;
    private final TransactionTemplate transacao;
    private final PrazosDaSaga prazos;

    public OrquestradorSagaOrdemServico(
            SagaOrdemServicoRepository sagas,
            OrdemDeServicoRepository ordens,
            PublicadorMensagens publicador,
            FabricaEnvelopes fabrica,
            ApplicationEventPublisher eventos,
            JsonMapper jsonMapper,
            TransactionTemplate transacao,
            PrazosDaSaga prazos) {
        this.sagas = sagas;
        this.ordens = ordens;
        this.publicador = publicador;
        this.fabrica = fabrica;
        this.eventos = eventos;
        this.jsonMapper = jsonMapper;
        this.transacao = transacao;
        this.prazos = prazos;
    }

    // ------------------------------------------------------------------ acoes registradas

    @Override
    @Transactional
    public void iniciar(UUID ordemDeServicoId) {
        SagaOrdemServico saga = SagaOrdemServico.iniciar(ordemDeServicoId, LocalDateTime.now());
        sagas.salvar(saga);
        registrar(saga, Tipo.INICIO, null, "Saga iniciada na abertura da ordem de servico");
        log.info("Saga iniciada. sagaId={}, ordemServicoId={}", saga.getId(), ordemDeServicoId);
    }

    @Override
    @Transactional
    public void solicitarOrcamento(UUID ordemDeServicoId, DadosOrcamento dados) {
        SagaOrdemServico saga = obter(ordemDeServicoId);
        if (saga.getStatus() != StatusSaga.AGUARDANDO_DIAGNOSTICO) {
            throw new RegraDeNegocioException(
                    "Orcamento ja solicitado para esta ordem de servico (Saga em " + saga.getStatus() + ").");
        }
        OrdemDeServico ordem = ordem(saga);
        saga.solicitarOrcamento(pedidoDeOrcamento(ordem, dados), LocalDateTime.now(), prazos.resposta());
        sagas.salvar(saga);
        registrar(saga, Tipo.ACAO_REGISTRADA, null, "Diagnostico enviado para orcamento");
        enviarComandos(saga);
    }

    @Override
    @Transactional
    public void execucaoIniciada(UUID ordemDeServicoId) {
        SagaOrdemServico saga = obter(ordemDeServicoId);
        if (saga.getStatus() != StatusSaga.AGUARDANDO_EXECUCAO) {
            ignorar(saga, null, "Execucao iniciada com a Saga em " + saga.getStatus());
            return;
        }
        saga.execucaoIniciada(LocalDateTime.now());
        sagas.salvar(saga);
        registrar(saga, Tipo.ACAO_REGISTRADA, null, "Execucao iniciada pelo mecanico");
    }

    @Override
    @Transactional
    public void execucaoConcluida(UUID ordemDeServicoId) {
        SagaOrdemServico saga = obter(ordemDeServicoId);
        if (saga.getStatus() != StatusSaga.EM_EXECUCAO) {
            ignorar(saga, null, "Execucao concluida com a Saga em " + saga.getStatus());
            return;
        }
        saga.execucaoConcluida(LocalDateTime.now());
        sagas.salvar(saga);
        registrar(saga, Tipo.ACAO_REGISTRADA, null, "Execucao concluida; Saga CONCLUIDA");
    }

    @Override
    @Transactional
    public void execucaoFalhou(UUID ordemDeServicoId, String motivo) {
        SagaOrdemServico saga = obter(ordemDeServicoId);
        if (saga.getStatus() != StatusSaga.EM_EXECUCAO) {
            ignorar(saga, null, "Execucao abortada com a Saga em " + saga.getStatus());
            return;
        }
        saga.execucaoFalhou(LocalDateTime.now(), prazos.resposta());
        sagas.salvar(saga);
        registrar(saga, Tipo.ACAO_REGISTRADA, null, "Execucao abortada pelo mecanico: " + motivo);
        enviarComandos(saga);
    }

    // ------------------------------------------------------------------ eventos dos participantes

    /** Chamado pelo consumidor de mensagens, dentro da transacao dele. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void tratar(Envelope evento) {
        SagaOrdemServico saga = obter(evento.ordemServicoId());
        switch (evento.type()) {
            case MensagensSaga.ORCAMENTO_GERADO -> aoGerarOrcamento(saga, evento);
            case MensagensSaga.ORCAMENTO_FALHOU -> aoFalharOrcamento(saga, evento);
            case MensagensSaga.ORCAMENTO_APROVADO -> aoDecidirOrcamento(saga, evento, true);
            case MensagensSaga.ORCAMENTO_REJEITADO -> aoDecidirOrcamento(saga, evento, false);
            case MensagensSaga.PECAS_RESERVADAS -> aoReservarPecas(saga, evento);
            case MensagensSaga.RESERVA_FALHOU -> aoFalharReserva(saga, evento);
            case MensagensSaga.PAGAMENTO_SOLICITADO -> aoSolicitarPagamento(saga, evento);
            case MensagensSaga.PAGAMENTO_CONFIRMADO -> aoConfirmarPagamento(saga, evento);
            case MensagensSaga.PAGAMENTO_RECUSADO -> aoNaoConcluirPagamento(saga, evento, MotivoEncerramento.PAGAMENTO_RECUSADO);
            case MensagensSaga.PAGAMENTO_EXPIRADO -> aoNaoConcluirPagamento(saga, evento, MotivoEncerramento.PAGAMENTO_EXPIRADO);
            case MensagensSaga.ESTORNO_FALHOU -> aoFalharEstorno(saga, evento);
            case MensagensSaga.APROVACAO_REVERTIDA, MensagensSaga.ORCAMENTO_CANCELADO,
                    MensagensSaga.RESERVA_LIBERADA, MensagensSaga.PAGAMENTO_ESTORNADO -> aoConfirmarCompensacao(saga, evento);
            default -> ignorar(saga, evento, "Evento sem tratamento no orquestrador");
        }
    }

    private void aoGerarOrcamento(SagaOrdemServico saga, Envelope evento) {
        MensagensSaga.OrcamentoGerado gerado = ler(evento, MensagensSaga.OrcamentoGerado.class);
        OrdemDeServico ordem = ordem(saga);
        if (saga.getStatus() != StatusSaga.GERANDO_ORCAMENTO || ordem.getStatus() != StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO) {
            ignorar(saga, evento, "Orcamento gerado com a Saga em " + saga.getStatus() + " e a OS em " + ordem.getStatus());
            return;
        }
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime prazo = prazos.decisao(local(gerado.validade()), agora);
        saga.orcamentoGerado(gerado.orcamentoId(), prazo, agora);
        moverOrdem(saga, ordem, OrigemAlteracaoStatus.SAGA, OrdemDeServico::enviarParaAprovacao);
        sagas.salvar(saga);
        registrar(saga, Tipo.EVENTO_RECEBIDO, evento.messageId(),
                evento.type() + " " + gerado.numeroOrcamento() + " (total " + gerado.valorTotal() + "); decisao do cliente ate " + prazo);
    }

    /**
     * Orcamento nao gerado. Com peca invalida o diagnostico volta ao mecanico (compensacao
     * {@code ReabrirDiagnostico}, local enquanto o diagnostico mora no OS); sem peca invalida (dado
     * financeiro recusado) a OS fica com o diagnostico concluido e o atendente pode reenviar.
     */
    private void aoFalharOrcamento(SagaOrdemServico saga, Envelope evento) {
        MensagensSaga.OrcamentoFalhou falha = ler(evento, MensagensSaga.OrcamentoFalhou.class);
        OrdemDeServico ordem = ordem(saga);
        if (saga.getStatus() != StatusSaga.GERANDO_ORCAMENTO) {
            ignorar(saga, evento, "Falha de orcamento com a Saga em " + saga.getStatus());
            return;
        }
        saga.orcamentoFalhou(falha.motivo(), LocalDateTime.now());
        boolean reabrir = falha.pecasInvalidas() != null && !falha.pecasInvalidas().isEmpty()
                && ordem.getStatus() == StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO;
        if (reabrir) {
            moverOrdem(saga, ordem, OrigemAlteracaoStatus.SAGA, OrdemDeServico::reabrirDiagnostico);
        }
        sagas.salvar(saga);
        registrar(saga, Tipo.EVENTO_RECEBIDO, evento.messageId(), evento.type() + ": " + falha.motivo()
                + (reabrir ? "; diagnostico reaberto" : "; diagnostico mantido, envio para orcamento pode ser refeito"));
    }

    private void aoDecidirOrcamento(SagaOrdemServico saga, Envelope evento, boolean aprovado) {
        MensagensSaga.OrcamentoDecidido decisao = ler(evento, MensagensSaga.OrcamentoDecidido.class);
        OrdemDeServico ordem = ordem(saga);
        if (saga.getStatus() != StatusSaga.AGUARDANDO_DECISAO || ordem.getStatus() != StatusOrdemDeServico.AGUARDANDO_APROVACAO) {
            ignorar(saga, evento, "Decisao com a Saga em " + saga.getStatus() + " e a OS em " + ordem.getStatus());
            return;
        }
        LocalDateTime agora = LocalDateTime.now();
        if (aprovado) {
            // A OS so muda quando as pecas estiverem reservadas (PecasReservadas).
            saga.orcamentoAprovado(decisao.orcamentoId(), itensDaOrdem(ordem), agora, prazos.resposta());
        } else {
            saga.orcamentoRejeitado(decisao.orcamentoId(), agora);
            moverOrdem(saga, ordem, OrigemAlteracaoStatus.SAGA, OrdemDeServico::recusarOrcamento);
        }
        sagas.salvar(saga);
        registrar(saga, Tipo.EVENTO_RECEBIDO, evento.messageId(), evento.type() + " (canal " + decisao.canal() + ")");
        enviarComandos(saga);
    }

    private void aoReservarPecas(SagaOrdemServico saga, Envelope evento) {
        OrdemDeServico ordem = ordem(saga);
        if (saga.getStatus() != StatusSaga.RESERVANDO_PECAS || ordem.getStatus() != StatusOrdemDeServico.AGUARDANDO_APROVACAO) {
            ignorar(saga, evento, "Reserva com a Saga em " + saga.getStatus() + " e a OS em " + ordem.getStatus());
            return;
        }
        Instant expiraEm = Instant.now().plus(prazos.pagamento());
        saga.pecasReservadas(expiraEm, LocalDateTime.now(), prazos.resposta());
        moverOrdem(saga, ordem, OrigemAlteracaoStatus.SAGA, OrdemDeServico::aguardarPagamento);
        sagas.salvar(saga);
        registrar(saga, Tipo.EVENTO_RECEBIDO, evento.messageId(), evento.type() + "; pagamento ate " + local(expiraEm));
        enviarComandos(saga);
    }

    private void aoFalharReserva(SagaOrdemServico saga, Envelope evento) {
        MensagensSaga.ReservaFalhou falha = ler(evento, MensagensSaga.ReservaFalhou.class);
        if (saga.getStatus() != StatusSaga.RESERVANDO_PECAS) {
            ignorar(saga, evento, "Falha de reserva com a Saga em " + saga.getStatus());
            return;
        }
        String motivo = "Estoque insuficiente: " + falha.faltantes().stream()
                .map(f -> f.pecaInsumoId() + " (solicitada " + f.solicitada() + ", disponivel " + f.disponivel() + ")")
                .collect(Collectors.joining(", "));
        saga.reservaFalhou(motivo, falha.faltantes(), LocalDateTime.now(), prazos.resposta());
        sagas.salvar(saga);
        registrar(saga, Tipo.EVENTO_RECEBIDO, evento.messageId(), evento.type() + ": " + motivo + "; aprovacao sera revertida");
        enviarComandos(saga);
    }

    private void aoSolicitarPagamento(SagaOrdemServico saga, Envelope evento) {
        MensagensSaga.PagamentoSolicitado solicitado = ler(evento, MensagensSaga.PagamentoSolicitado.class);
        if (!saga.aguardandoCobranca()) {
            ignorar(saga, evento, "Cobranca com a Saga em " + saga.getStatus()
                    + (saga.getPagamentoId() != null ? " e pagamento " + saga.getPagamentoId() + " ja registrado" : ""));
            return;
        }
        LocalDateTime limite = local(solicitado.expiraEm()).plus(prazos.margemPagamento());
        saga.pagamentoSolicitado(solicitado.pagamentoId(), solicitado.linkPagamento(), limite, LocalDateTime.now());
        sagas.salvar(saga);
        registrar(saga, Tipo.EVENTO_RECEBIDO, evento.messageId(), evento.type() + " " + solicitado.valor() + " ate "
                + local(solicitado.expiraEm()) + ": " + solicitado.linkPagamento());
    }

    private void aoConfirmarPagamento(SagaOrdemServico saga, Envelope evento) {
        MensagensSaga.PagamentoConfirmado confirmado = ler(evento, MensagensSaga.PagamentoConfirmado.class);
        OrdemDeServico ordem = ordem(saga);
        if (!pagamentoEsperado(saga, ordem, confirmado.pagamentoId())) {
            ignorar(saga, evento, "Pagamento confirmado com a Saga em " + saga.getStatus() + " e a OS em " + ordem.getStatus());
            return;
        }
        saga.pagamentoConfirmado(confirmado.pagamentoId(), LocalDateTime.now());
        moverOrdem(saga, ordem, OrigemAlteracaoStatus.SAGA, OrdemDeServico::confirmarPagamento);
        sagas.salvar(saga);
        registrar(saga, Tipo.EVENTO_RECEBIDO, evento.messageId(), evento.type() + " " + confirmado.valor());
    }

    private void aoNaoConcluirPagamento(SagaOrdemServico saga, Envelope evento, MotivoEncerramento motivo) {
        UUID pagamentoId = jsonMapper.treeToValue(evento.payload().get("pagamentoId"), UUID.class);
        OrdemDeServico ordem = ordem(saga);
        if (!pagamentoEsperado(saga, ordem, pagamentoId)) {
            ignorar(saga, evento, "Pagamento nao concluido com a Saga em " + saga.getStatus() + " e a OS em " + ordem.getStatus());
            return;
        }
        String detalhe = evento.payload().hasNonNull("motivo") ? ": " + evento.payload().get("motivo").asString() : "";
        cancelarPorPagamento(saga, ordem, motivo, OrigemAlteracaoStatus.SAGA);
        sagas.salvar(saga);
        registrar(saga, Tipo.EVENTO_RECEBIDO, evento.messageId(), evento.type() + detalhe + "; reserva e orcamento serao desfeitos");
        enviarComandos(saga);
    }

    private void aoFalharEstorno(SagaOrdemServico saga, Envelope evento) {
        MensagensSaga.EstornoFalhou falha = ler(evento, MensagensSaga.EstornoFalhou.class);
        if (saga.getStatus() != StatusSaga.COMPENSANDO || !saga.getCompensacoesPendentes().contains(MensagensSaga.PAGAMENTO_ESTORNADO)) {
            ignorar(saga, evento, "Estorno falhou com a Saga em " + saga.getStatus());
            return;
        }
        saga.compensacaoFalhou(MensagensSaga.PAGAMENTO_ESTORNADO, LocalDateTime.now(), prazos.resposta());
        sagas.salvar(saga);
        registrar(saga, Tipo.EVENTO_RECEBIDO, evento.messageId(), evento.type() + ": " + falha.motivo()
                + "; novo estorno quando o prazo vencer (tentativa " + (saga.getTentativasPasso() + 1) + " de " + prazos.maxTentativas() + ")");
    }

    private void aoConfirmarCompensacao(SagaOrdemServico saga, Envelope evento) {
        boolean emCompensacao = saga.getStatus() == StatusSaga.COMPENSANDO || saga.getStatus() == StatusSaga.REVERTENDO_APROVACAO;
        if (!emCompensacao || !saga.getCompensacoesPendentes().contains(evento.type())) {
            ignorar(saga, evento, "Confirmacao " + evento.type() + " com a Saga em " + saga.getStatus());
            return;
        }
        LocalDateTime agora = LocalDateTime.now();
        saga.compensacaoConfirmada(evento.type(), agora, prazos.decisao(null, agora));
        sagas.salvar(saga);
        registrar(saga, Tipo.EVENTO_RECEBIDO, evento.messageId(), evento.type() + "; Saga em " + saga.getStatus());
    }

    // ------------------------------------------------------------------ prazos

    /** Processa as Sagas com prazo vencido, cada uma na sua transacao. */
    public int processarPrazosVencidos() {
        return processarPrazosVencidos(LocalDateTime.now());
    }

    /** {@code agora} explicito para os testes avancarem o relogio. */
    public int processarPrazosVencidos(LocalDateTime agora) {
        List<UUID> vencidas = sagas.buscarIdsComPrazoVencido(agora);
        int processadas = 0;
        for (UUID sagaId : vencidas) {
            try {
                transacao.executeWithoutResult(status -> processarPrazo(sagaId, agora));
                processadas++;
            } catch (RuntimeException e) {
                // Conflito de versao com o consumidor de mensagens, por exemplo: o proximo ciclo tenta de novo.
                log.warn("Falha ao processar prazo da Saga; nova tentativa no proximo ciclo. sagaId={}, erro={}", sagaId, e.toString());
            }
        }
        return processadas;
    }

    private void processarPrazo(UUID sagaId, LocalDateTime agora) {
        SagaOrdemServico saga = sagas.buscarPorId(sagaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Saga nao encontrada: " + sagaId));
        if (!saga.prazoVencido(agora)) {
            return;
        }
        switch (saga.getStatus()) {
            case AGUARDANDO_DECISAO -> vencerDecisao(saga, agora);
            case GERANDO_ORCAMENTO -> reenviarOuDesistir(saga, agora, () -> {
                saga.orcamentoFalhou("Billing nao respondeu ao GerarOrcamento", agora);
                registrar(saga, Tipo.TIMEOUT, null, "GerarOrcamento sem resposta apos os reenvios; envio para orcamento pode ser refeito");
            });
            case RESERVANDO_PECAS -> reenviarOuDesistir(saga, agora, () -> {
                saga.reservaSemResposta(agora, prazos.resposta());
                registrar(saga, Tipo.TIMEOUT, null, "ReservarPecas sem resposta apos os reenvios; reserva liberada e aprovacao revertida");
            });
            case AGUARDANDO_PAGAMENTO -> {
                if (saga.aguardandoCobranca()) {
                    reenviarOuDesistir(saga, agora, () -> vencerPagamento(saga, agora, "SolicitarPagamento sem resposta apos os reenvios"));
                } else {
                    vencerPagamento(saga, agora, "Prazo de pagamento + margem vencido sem resposta do Billing");
                }
            }
            case COMPENSANDO, REVERTENDO_APROVACAO -> {
                boolean reenviou = saga.respostaDaCompensacaoNaoChegou(agora, prazos.resposta(), prazos.maxTentativas());
                registrar(saga, Tipo.TIMEOUT, null, reenviou
                        ? "Confirmacao nao chegou; reenvio " + saga.getTentativasPasso() + " de " + prazos.maxTentativas()
                        : "Confirmacao nao chegou e as tentativas acabaram: FALHA_COMPENSACAO");
                if (!reenviou) {
                    log.error("Saga em FALHA_COMPENSACAO: exige acao manual. sagaId={}, ordemServicoId={}, pendentes={}",
                            saga.getId(), saga.getOrdemDeServicoId(), saga.getCompensacoesPendentes());
                }
            }
            default -> {
                saga.descartarPrazo(agora);
                registrar(saga, Tipo.TIMEOUT, null, "Prazo vencido sem acao prevista em " + saga.getStatus());
            }
        }
        sagas.salvar(saga);
        enviarComandos(saga);
    }

    private void reenviarOuDesistir(SagaOrdemServico saga, LocalDateTime agora, Runnable desistir) {
        if (saga.reenviarComandoDeAvanco(agora, prazos.resposta(), prazos.maxTentativas())) {
            registrar(saga, Tipo.TIMEOUT, null, "Resposta nao chegou; reenvio " + saga.getTentativasPasso() + " de " + prazos.maxTentativas());
            return;
        }
        desistir.run();
    }

    private void vencerDecisao(SagaOrdemServico saga, LocalDateTime agora) {
        OrdemDeServico ordem = ordem(saga);
        if (ordem.getStatus() != StatusOrdemDeServico.AGUARDANDO_APROVACAO) {
            saga.descartarPrazo(agora);
            registrar(saga, Tipo.TIMEOUT, null, "Prazo de decisao vencido, mas a OS esta em " + ordem.getStatus());
            return;
        }
        saga.prazoDecisaoVencido(agora, prazos.resposta());
        moverOrdem(saga, ordem, OrigemAlteracaoStatus.TIMEOUT, OrdemDeServico::expirarOrcamento);
        registrar(saga, Tipo.TIMEOUT, null, "Prazo de decisao vencido: orcamento sera cancelado");
    }

    /** Rede de seguranca do pagamento: o Billing e a autoridade, mas nada chegou ate o prazo + margem. */
    private void vencerPagamento(SagaOrdemServico saga, LocalDateTime agora, String detalhe) {
        OrdemDeServico ordem = ordem(saga);
        if (ordem.getStatus() != StatusOrdemDeServico.AGUARDANDO_PAGAMENTO) {
            saga.descartarPrazo(agora);
            registrar(saga, Tipo.TIMEOUT, null, "Prazo de pagamento vencido, mas a OS esta em " + ordem.getStatus());
            return;
        }
        cancelarPorPagamento(saga, ordem, MotivoEncerramento.PAGAMENTO_EXPIRADO, OrigemAlteracaoStatus.TIMEOUT);
        registrar(saga, Tipo.TIMEOUT, null, detalhe + ": tratado como PagamentoExpirado");
    }

    // ------------------------------------------------------------------ apoio

    private void cancelarPorPagamento(SagaOrdemServico saga, OrdemDeServico ordem, MotivoEncerramento motivo, OrigemAlteracaoStatus origem) {
        saga.pagamentoNaoConcluido(motivo, LocalDateTime.now(), prazos.resposta());
        moverOrdem(saga, ordem, origem, os -> os.cancelarPorPagamento(motivo));
    }

    /** Evento de pagamento so vale para a Saga esperando pagamento e para a cobranca que ela registrou. */
    private static boolean pagamentoEsperado(SagaOrdemServico saga, OrdemDeServico ordem, UUID pagamentoId) {
        return saga.getStatus() == StatusSaga.AGUARDANDO_PAGAMENTO
                && ordem.getStatus() == StatusOrdemDeServico.AGUARDANDO_PAGAMENTO
                && (saga.getPagamentoId() == null || saga.getPagamentoId().equals(pagamentoId));
    }

    /** Snapshot do diagnostico e dos dados da OS: o Billing nao consulta cliente nem OS (ADR 0003). */
    private MensagensSaga.GerarOrcamento pedidoDeOrcamento(OrdemDeServico ordem, DadosOrcamento dados) {
        List<String> descricoes = ordem.getServicos().stream().map(ServicoOrdem::getDescricao).toList();
        return new MensagensSaga.GerarOrcamento(
                ordem.getNumeroOrdemServico(),
                new MensagensSaga.Cliente(
                        ordem.getCliente().getId(),
                        ordem.getCliente().getNome(),
                        ordem.getCliente().getCpfOuCnpj(),
                        ordem.getCliente().getTipoCliente() == null ? null : ordem.getCliente().getTipoCliente().name(),
                        ordem.getCliente().getEmail()),
                new MensagensSaga.Veiculo(
                        ordem.getVeiculo().getPlaca(),
                        ordem.getVeiculo().getMarca(),
                        ordem.getVeiculo().getModelo(),
                        ordem.getVeiculo().getAno() > 0 ? ordem.getVeiculo().getAno() : null),
                new MensagensSaga.Funcionario(ordem.getFuncionario().getId(), ordem.getFuncionario().getNome()),
                descricoes.isEmpty() ? "Pecas previstas no diagnostico" : String.join("; ", descricoes),
                ordem.getServicos().stream()
                        .map(servico -> new MensagensSaga.Servico(servico.getDescricao(), dinheiro(servico.getValorMaoDeObra())))
                        .toList(),
                itensDaOrdem(ordem),
                dinheiro(dados.valorMaoDeObra()),
                dinheiro(dados.desconto()),
                dados.validade() == null ? null : dados.validade().atZone(FUSO).toInstant(),
                dados.observacoes());
    }

    /** Contrato: dinheiro como texto com duas casas ("200.00", nunca "200" ou "0"). */
    private static BigDecimal dinheiro(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP);
    }

    /** Pecas previstas da OS, somando repeticoes da mesma peca. */
    private static List<MensagensSaga.ItemPeca> itensDaOrdem(OrdemDeServico ordem) {
        Map<String, Integer> quantidades = new LinkedHashMap<>();
        for (PecaPrevistaOrdem peca : ordem.getPecasPrevistas()) {
            quantidades.merge(peca.getPecaInsumoId(), peca.getQuantidade(), Integer::sum);
        }
        return quantidades.entrySet().stream()
                .map(item -> new MensagensSaga.ItemPeca(item.getKey(), item.getValue()))
                .toList();
    }

    /** Saga da OS; se a OS e anterior a Saga, adota (estado derivado do status atual da ordem). */
    private SagaOrdemServico obter(UUID ordemDeServicoId) {
        return sagas.buscarPorOrdemDeServico(ordemDeServicoId).orElseGet(() -> {
            OrdemDeServico ordem = ordens.buscarPorId(ordemDeServicoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada: " + ordemDeServicoId));
            SagaOrdemServico adotada = SagaOrdemServico.adotar(
                    ordemDeServicoId, ordem.getStatus(), ordem.getMotivoEncerramento(), LocalDateTime.now());
            sagas.salvar(adotada);
            registrar(adotada, Tipo.ADOCAO, null, "Saga adotada para OS existente em " + ordem.getStatus());
            return adotada;
        });
    }

    private OrdemDeServico ordem(SagaOrdemServico saga) {
        return ordens.buscarPorId(saga.getOrdemDeServicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada: " + saga.getOrdemDeServicoId()));
    }

    private void moverOrdem(SagaOrdemServico saga, OrdemDeServico ordem, OrigemAlteracaoStatus origem, Consumer<OrdemDeServico> transicao) {
        SituacaoOrdemDeServico situacaoAnterior = ordem.getSituacao();
        LocalDateTime anteriorDesde = ordem.getSituacaoAlteradaEm();
        ordem.registrarOrigemDasAlteracoes(origem, saga.getId());
        transicao.accept(ordem);
        ordens.salvar(ordem);
        // Mesmo evento que as acoes HTTP publicam: notificacao ao cliente e metricas continuam iguais.
        if (situacaoAnterior != ordem.getSituacao()) {
            eventos.publishEvent(StatusOrdemDeServicoAlterado.de(ordem, situacaoAnterior, anteriorDesde));
        }
    }

    private void enviarComandos(SagaOrdemServico saga) {
        for (ComandoSaga comando : saga.descarregarComandos()) {
            if (comando.reenvio()) {
                publicador.reenviar(comando.fila(), comando.tipo(), saga.getOrdemDeServicoId()).ifPresentOrElse(
                        envelope -> registrar(saga, Tipo.COMANDO_REENVIADO, envelope.messageId(), comando.tipo()),
                        () -> {
                            log.warn("Comando sem envio anterior para reenviar. sagaId={}, tipo={}", saga.getId(), comando.tipo());
                            registrar(saga, Tipo.TIMEOUT, null, "Nenhum " + comando.tipo() + " anterior para reenviar");
                        });
                continue;
            }
            Envelope envelope = fabrica.novo(comando.tipo(), saga.getId(), saga.getOrdemDeServicoId(), comando.payload());
            publicador.publicar(comando.fila(), envelope);
            registrar(saga, Tipo.COMANDO_ENVIADO, envelope.messageId(), comando.tipo());
        }
    }

    private void ignorar(SagaOrdemServico saga, Envelope evento, String motivo) {
        log.warn("Evento ignorado pela Saga. sagaId={}, tipo={}, motivo={}",
                saga.getId(), evento == null ? null : evento.type(), motivo);
        registrar(saga, Tipo.EVENTO_IGNORADO, evento == null ? null : evento.messageId(),
                (evento == null ? "" : evento.type() + ": ") + motivo);
    }

    private void registrar(SagaOrdemServico saga, Tipo tipo, UUID mensagemId, String detalhe) {
        PassoSaga passo = saga.getPassoAtual();
        sagas.registrarPasso(new RegistroPassoSaga(saga.getId(), passo, tipo, mensagemId, detalhe, LocalDateTime.now()));
    }

    private <T> T ler(Envelope evento, Class<T> tipo) {
        return jsonMapper.treeToValue(evento.payload(), tipo);
    }

    private static LocalDateTime local(Instant instante) {
        return instante == null ? null : LocalDateTime.ofInstant(instante, FUSO);
    }
}
