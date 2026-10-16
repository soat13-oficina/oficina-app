package br.com.oficina.ordemservico.saga.application;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.OrigemAlteracaoStatus;
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
 *       orcamento, inicio e conclusao da execucao): o servico ja moveu a ordem; aqui so a Saga avanca;</li>
 *   <li><b>eventos</b> dos participantes, por mensagem (decisao e cancelamento do orcamento): aqui
 *       a Saga avanca, a ordem e movida com origem {@code SAGA} e os proximos comandos vao ao outbox;</li>
 *   <li><b>prazos</b> vencidos, pelo agendador: decisao do cliente e resposta das compensacoes.</li>
 * </ul>
 * Evento que nao cabe no estado atual e registrado como {@code EVENTO_IGNORADO} e confirmado - nunca
 * reprocessado em laco (ADR 0004, regra 3).
 */
@Service
public class OrquestradorSagaOrdemServico implements PassosDaSaga {
    private static final Logger log = LoggerFactory.getLogger(OrquestradorSagaOrdemServico.class);

    private final SagaOrdemServicoRepository sagas;
    private final OrdemDeServicoRepository ordens;
    private final PublicadorMensagens publicador;
    private final FabricaEnvelopes fabrica;
    private final ApplicationEventPublisher eventos;
    private final JsonMapper jsonMapper;
    private final TransactionTemplate transacao;
    private final Duration prazoDecisao;
    private final Duration prazoDecisaoPadrao;
    private final Duration prazoResposta;
    private final int maxTentativas;

    public OrquestradorSagaOrdemServico(
            SagaOrdemServicoRepository sagas,
            OrdemDeServicoRepository ordens,
            PublicadorMensagens publicador,
            FabricaEnvelopes fabrica,
            ApplicationEventPublisher eventos,
            JsonMapper jsonMapper,
            TransactionTemplate transacao,
            @Value("${saga.prazo-decisao:#{null}}") Duration prazoDecisao,
            @Value("${saga.prazo-decisao-padrao:P7D}") Duration prazoDecisaoPadrao,
            @Value("${saga.prazo-resposta:PT5M}") Duration prazoResposta,
            @Value("${saga.max-tentativas:3}") int maxTentativas) {
        this.sagas = sagas;
        this.ordens = ordens;
        this.publicador = publicador;
        this.fabrica = fabrica;
        this.eventos = eventos;
        this.jsonMapper = jsonMapper;
        this.transacao = transacao;
        this.prazoDecisao = prazoDecisao;
        this.prazoDecisaoPadrao = prazoDecisaoPadrao;
        this.prazoResposta = prazoResposta;
        this.maxTentativas = maxTentativas;
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

    /** Id da Saga da OS (adotando, se a OS e anterior a Saga) - vai junto do orcamento criado para ela. */
    @Override
    @Transactional
    public UUID sagaIdDaOrdem(UUID ordemDeServicoId) {
        return obter(ordemDeServicoId).getId();
    }

    /** Envio do diagnostico para orcamento (ainda sincrono no OS Service): comeca a espera pela decisao. */
    @Override
    @Transactional
    public void orcamentoGerado(UUID ordemDeServicoId, UUID orcamentoId, LocalDateTime validadeOrcamento) {
        SagaOrdemServico saga = obter(ordemDeServicoId);
        LocalDateTime agora = LocalDateTime.now();
        if (saga.getStatus() != StatusSaga.AGUARDANDO_DIAGNOSTICO && saga.getStatus() != StatusSaga.GERANDO_ORCAMENTO) {
            ignorar(saga, null, "Orcamento gerado com a Saga em " + saga.getStatus());
            return;
        }
        LocalDateTime prazo = calcularPrazoDecisao(validadeOrcamento, agora);
        saga.orcamentoGerado(orcamentoId, prazo, agora);
        sagas.salvar(saga);
        registrar(saga, Tipo.ACAO_REGISTRADA, null, "Orcamento gerado; decisao do cliente ate " + prazo);
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

    // ------------------------------------------------------------------ eventos dos participantes

    /** Chamado pelo consumidor de mensagens, dentro da transacao dele. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void tratar(Envelope evento) {
        SagaOrdemServico saga = obter(evento.ordemServicoId());
        switch (evento.type()) {
            case MensagensSaga.ORCAMENTO_APROVADO -> aoDecidirOrcamento(saga, evento, true);
            case MensagensSaga.ORCAMENTO_REJEITADO -> aoDecidirOrcamento(saga, evento, false);
            case MensagensSaga.ORCAMENTO_CANCELADO -> aoConfirmarCompensacao(saga, evento);
            default -> ignorar(saga, evento, "Evento sem tratamento no orquestrador");
        }
    }

    private void aoDecidirOrcamento(SagaOrdemServico saga, Envelope evento, boolean aprovado) {
        MensagensSaga.OrcamentoDecidido decisao = jsonMapper.treeToValue(evento.payload(), MensagensSaga.OrcamentoDecidido.class);
        OrdemDeServico ordem = ordem(saga);
        if (saga.getStatus() != StatusSaga.AGUARDANDO_DECISAO || ordem.getStatus() != StatusOrdemDeServico.AGUARDANDO_APROVACAO) {
            ignorar(saga, evento, "Decisao com a Saga em " + saga.getStatus() + " e a OS em " + ordem.getStatus());
            return;
        }
        LocalDateTime agora = LocalDateTime.now();
        if (aprovado) {
            saga.orcamentoAprovado(decisao.orcamentoId(), agora);
            moverOrdem(saga, ordem, OrigemAlteracaoStatus.SAGA, OrdemDeServico::aprovarOrcamento);
        } else {
            saga.orcamentoRejeitado(decisao.orcamentoId(), agora);
            moverOrdem(saga, ordem, OrigemAlteracaoStatus.SAGA, OrdemDeServico::recusarOrcamento);
        }
        sagas.salvar(saga);
        registrar(saga, Tipo.EVENTO_RECEBIDO, evento.messageId(), evento.type() + " (canal " + decisao.canal() + ")");
        enviarComandos(saga);
    }

    private void aoConfirmarCompensacao(SagaOrdemServico saga, Envelope evento) {
        if (saga.getStatus() != StatusSaga.COMPENSANDO || !saga.getCompensacoesPendentes().contains(evento.type())) {
            ignorar(saga, evento, "Confirmacao " + evento.type() + " com a Saga em " + saga.getStatus());
            return;
        }
        saga.compensacaoConfirmada(evento.type(), LocalDateTime.now());
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
            case COMPENSANDO -> {
                boolean reenviou = saga.respostaDaCompensacaoNaoChegou(agora, prazoResposta, maxTentativas);
                registrar(saga, Tipo.TIMEOUT, null, reenviou
                        ? "Confirmacao nao chegou; reenvio " + saga.getTentativasPasso() + " de " + maxTentativas
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

    private void vencerDecisao(SagaOrdemServico saga, LocalDateTime agora) {
        OrdemDeServico ordem = ordem(saga);
        if (ordem.getStatus() != StatusOrdemDeServico.AGUARDANDO_APROVACAO) {
            saga.descartarPrazo(agora);
            registrar(saga, Tipo.TIMEOUT, null, "Prazo de decisao vencido, mas a OS esta em " + ordem.getStatus());
            return;
        }
        saga.prazoDecisaoVencido(agora, prazoResposta);
        moverOrdem(saga, ordem, OrigemAlteracaoStatus.TIMEOUT, OrdemDeServico::expirarOrcamento);
        registrar(saga, Tipo.TIMEOUT, null, "Prazo de decisao vencido: orcamento sera cancelado");
    }

    // ------------------------------------------------------------------ apoio

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
        eventos.publishEvent(StatusOrdemDeServicoAlterado.de(ordem, situacaoAnterior, anteriorDesde));
    }

    private void enviarComandos(SagaOrdemServico saga) {
        for (ComandoSaga comando : saga.descarregarComandos()) {
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

    /**
     * {@code saga.prazo-decisao} (curto em hml, para a demonstracao) tem prioridade; senao vale a
     * validade do orcamento; sem validade, {@code saga.prazo-decisao-padrao}.
     */
    private LocalDateTime calcularPrazoDecisao(LocalDateTime validadeOrcamento, LocalDateTime agora) {
        if (prazoDecisao != null) {
            return agora.plus(prazoDecisao);
        }
        return validadeOrcamento != null ? validadeOrcamento : agora.plus(prazoDecisaoPadrao);
    }
}
