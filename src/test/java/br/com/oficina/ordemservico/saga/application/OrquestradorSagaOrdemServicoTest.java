package br.com.oficina.ordemservico.saga.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;
import br.com.oficina.common.infrastructure.mensageria.SpringDataOutboxMensagemRepository;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.orcamento.infrastructure.persistence.SpringDataOrcamentoRepository;
import br.com.oficina.ordemservico.domain.model.AlteracaoStatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.MotivoEncerramento;
import br.com.oficina.ordemservico.domain.model.OrigemAlteracaoStatus;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.HistoricoStatusOrdemDeServicoRepository;
import br.com.oficina.ordemservico.saga.domain.model.MensagensSaga;
import br.com.oficina.ordemservico.saga.domain.model.RegistroPassoSaga;
import br.com.oficina.ordemservico.saga.domain.model.SagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.model.StatusSaga;
import br.com.oficina.ordemservico.saga.domain.repository.SagaOrdemServicoRepository;
import br.com.oficina.ordemservico.saga.infrastructure.persistence.SpringDataSagaOrdemServicoRepository;
import br.com.oficina.support.saga.FluxoOficinaHttp;

/**
 * Orquestrador ponta a ponta dentro do monolito: APIs HTTP reais, outbox, transporte em memoria,
 * consumidor idempotente, orquestrador e o orcamento como participante (futuro Billing).
 */
@SpringBootTest
class OrquestradorSagaOrdemServicoTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private OrquestradorSagaOrdemServico orquestrador;

    @Autowired
    private SagaOrdemServicoRepository sagas;

    @Autowired
    private SpringDataSagaOrdemServicoRepository springDataSagas;

    @Autowired
    private HistoricoStatusOrdemDeServicoRepository historico;

    @Autowired
    private SpringDataOrcamentoRepository orcamentos;

    @Autowired
    private SpringDataOutboxMensagemRepository outbox;

    @Autowired
    private PublicadorMensagens publicadorMensagens;

    @Autowired
    private FabricaEnvelopes fabrica;

    @Autowired
    private TransactionTemplate transacao;

    private FluxoOficinaHttp fluxo;

    @BeforeEach
    void preparar() {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        fluxo = new FluxoOficinaHttp(mvc, contexto);
        fluxo.limpar();
    }

    @AfterEach
    void limpar() {
        fluxo.limpar();
    }

    private SagaOrdemServico saga(String numero) {
        return sagas.buscarPorOrdemDeServico(fluxo.ordem(numero).getId()).orElseThrow();
    }

    private List<RegistroPassoSaga.Tipo> tiposDosPassos(String numero) {
        return sagas.listarPassos(saga(numero).getId()).stream().map(RegistroPassoSaga::getTipo).toList();
    }

    private String ateAguardarAprovacao(String placa) throws Exception {
        String numero = fluxo.abrirOrdem(placa);
        fluxo.diagnosticar(numero, "Troca de oleo");
        return numero;
    }

    @Test
    void aberturaDaOrdemIniciaASaga() throws Exception {
        String numero = fluxo.abrirOrdem("SAG1A11");

        SagaOrdemServico saga = saga(numero);
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(tiposDosPassos(numero)).containsExactly(RegistroPassoSaga.Tipo.INICIO);
    }

    @Test
    void aprovacaoPeloClienteChegaPorMensagemEMoveAOrdemComOrigemSaga() throws Exception {
        String numero = ateAguardarAprovacao("SAG2A22");
        String orcamento = fluxo.enviarParaOrcamento(numero);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
        assertThat(saga(numero).getPrazoExpiraEm()).isEqualTo(LocalDateTime.of(2030, 1, 1, 0, 0));
        assertThat(orcamentos.findAll().get(0).getSagaId()).isEqualTo(saga(numero).getId());

        fluxo.clienteAprova(orcamento);
        assertThat(fluxo.ordem(numero).getStatus()).as("so depois do outbox").isEqualTo(StatusOrdemDeServico.AGUARDANDO_APROVACAO);

        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_EXECUCAO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_EXECUCAO);
        AlteracaoStatusOrdemDeServico ultima = ultimaAlteracao(numero);
        assertThat(ultima.origem()).isEqualTo(OrigemAlteracaoStatus.SAGA);
        assertThat(ultima.sagaId()).isEqualTo(saga(numero).getId());
        assertThat(sagas.listarPassos(saga(numero).getId()))
                .anySatisfy(p -> assertThat(p.getDetalhe()).contains("OrcamentoAprovado").contains("CLIENTE"));

        fluxo.acao(numero, "/execucao/iniciar");
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.EM_EXECUCAO);
        fluxo.acao(numero, "/servico/concluir");
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.CONCLUIDA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.SERVICO_CONCLUIDO);
    }

    @Test
    void rejeicaoEncerraAOrdemEASagaSemExecucao() throws Exception {
        String numero = ateAguardarAprovacao("SAG3A33");
        String orcamento = fluxo.enviarParaOrcamento(numero);

        fluxo.clienteRejeita(orcamento);
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.OS_FINALIZADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.ORCAMENTO_RECUSADO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
    }

    @Test
    void prazoDeDecisaoVencidoCancelaOOrcamentoPorCompensacaoEEncerraAOrdem() throws Exception {
        String numero = ateAguardarAprovacao("SAG4A44");
        fluxo.enviarParaOrcamento(numero);

        assertThat(orquestrador.processarPrazosVencidos(LocalDateTime.of(2029, 12, 31, 0, 0))).as("ainda no prazo").isZero();
        assertThat(orquestrador.processarPrazosVencidos(LocalDateTime.of(2030, 1, 2, 0, 0))).isEqualTo(1);

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.OS_FINALIZADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.ORCAMENTO_EXPIRADO);
        assertThat(ultimaAlteracao(numero).origem()).isEqualTo(OrigemAlteracaoStatus.TIMEOUT);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        assertThat(outbox.findAll()).extracting(m -> m.getTipo()).contains(MensagensSaga.CANCELAR_ORCAMENTO);

        fluxo.processarMensagens();

        assertThat(orcamentos.findAll().get(0).getStatus()).isEqualTo(StatusOrcamento.CANCELADO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(saga(numero).getCompensacoesPendentes()).isEmpty();
        assertThat(tiposDosPassos(numero)).contains(
                RegistroPassoSaga.Tipo.TIMEOUT, RegistroPassoSaga.Tipo.COMANDO_ENVIADO, RegistroPassoSaga.Tipo.EVENTO_RECEBIDO);
    }

    @Test
    void confirmacaoPerdidaEhPedidaDeNovoEOParticipanteRepublicaPeloEstado() throws Exception {
        String numero = ateAguardarAprovacao("SAG5A55");
        fluxo.enviarParaOrcamento(numero);
        orquestrador.processarPrazosVencidos(LocalDateTime.of(2030, 1, 2, 0, 0));
        fluxo.processarMensagens();
        // Simula a resposta perdida: a Saga volta a esperar a confirmacao, mas o orcamento ja esta cancelado.
        transacao.executeWithoutResult(s -> {
            SagaOrdemServico emCompensacao = SagaOrdemServico.iniciar(fluxo.ordem(numero).getId(), LocalDateTime.now());
            springDataSagas.deleteAllInBatch();
            emCompensacao.orcamentoGerado(orcamentos.findAll().get(0).getId(), LocalDateTime.of(2030, 1, 1, 0, 0), LocalDateTime.now());
            emCompensacao.prazoDecisaoVencido(LocalDateTime.of(2030, 1, 2, 0, 0), Duration.ofMinutes(5));
            emCompensacao.descarregarComandos();
            springDataSagas.save(emCompensacao);
        });

        assertThat(orquestrador.processarPrazosVencidos(LocalDateTime.of(2030, 1, 2, 0, 10))).isEqualTo(1);
        assertThat(saga(numero).getTentativasPasso()).isEqualTo(1);
        fluxo.processarMensagens();

        assertThat(saga(numero).getStatus()).as("o reenvio encontrou o orcamento ja cancelado e confirmou de novo")
                .isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
    }

    @Test
    void semConfirmacaoAposTodasAsTentativasASagaFicaEmFalhaDeCompensacao() throws Exception {
        String numero = ateAguardarAprovacao("SAG6A66");
        fluxo.enviarParaOrcamento(numero);
        LocalDateTime agora = LocalDateTime.of(2030, 1, 2, 0, 0);
        orquestrador.processarPrazosVencidos(agora);

        // O participante nunca responde: o outbox nao e processado.
        for (int i = 1; i <= 4; i++) {
            agora = agora.plusMinutes(10);
            orquestrador.processarPrazosVencidos(agora);
        }

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.FALHA_COMPENSACAO);
        assertThat(outbox.findAll()).filteredOn(m -> MensagensSaga.CANCELAR_ORCAMENTO.equals(m.getTipo())).hasSize(4);
        assertThat(orquestrador.processarPrazosVencidos(agora.plusDays(1))).as("estado final nao vence").isZero();
    }

    @Test
    void eventoQueNaoCabeNoEstadoAtualEhIgnoradoEConfirmado() throws Exception {
        String numero = ateAguardarAprovacao("SAG7A77");
        String orcamento = fluxo.enviarParaOrcamento(numero);
        fluxo.clienteRejeita(orcamento);
        fluxo.processarMensagens();
        UUID orcamentoId = orcamentos.findAll().get(0).getId();

        // Uma aprovacao atrasada (ex.: reenviada por um sistema externo) depois da rejeicao.
        publicarEvento(MensagensSaga.ORCAMENTO_APROVADO, fluxo.ordem(numero).getId(),
                Map.of("orcamentoId", orcamentoId.toString(), "canal", "INTEGRACAO", "decididoEm", "2026-10-15T13:00:00Z"));
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.OS_FINALIZADA);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(tiposDosPassos(numero)).contains(RegistroPassoSaga.Tipo.EVENTO_IGNORADO);
        assertThat(outbox.findAll()).allSatisfy(m -> assertThat(m.getPublicadaEm()).isNotNull());
    }

    @Test
    void ordemSemSagaEhAdotadaQuandoOPrimeiroEventoChega() throws Exception {
        String numero = ateAguardarAprovacao("SAG8A88");
        String orcamento = fluxo.enviarParaOrcamento(numero);
        transacao.executeWithoutResult(s -> springDataSagas.deleteAll());

        fluxo.clienteAprova(orcamento);
        fluxo.processarMensagens();

        assertThat(tiposDosPassos(numero)).startsWith(RegistroPassoSaga.Tipo.ADOCAO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_EXECUCAO);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_EXECUCAO);
    }

    @Test
    void acoesForaDoEstadoEsperadoSaoRegistradasComoIgnoradas() throws Exception {
        String numero = fluxo.abrirOrdem("SAG9A99");
        UUID ordemId = fluxo.ordem(numero).getId();

        orquestrador.execucaoIniciada(ordemId);
        orquestrador.execucaoConcluida(ordemId);
        orquestrador.orcamentoGerado(ordemId, UUID.randomUUID(), null);
        orquestrador.orcamentoGerado(ordemId, UUID.randomUUID(), null);

        assertThat(tiposDosPassos(numero)).containsExactly(
                RegistroPassoSaga.Tipo.INICIO,
                RegistroPassoSaga.Tipo.EVENTO_IGNORADO,
                RegistroPassoSaga.Tipo.EVENTO_IGNORADO,
                RegistroPassoSaga.Tipo.ACAO_REGISTRADA,
                RegistroPassoSaga.Tipo.EVENTO_IGNORADO);
        assertThat(saga(numero).getPrazoExpiraEm()).as("sem validade: prazo padrao de 7 dias")
                .isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    void consultaDaSagaMostraEstadoEHistoria() throws Exception {
        String numero = ateAguardarAprovacao("SAG1B11");
        fluxo.enviarParaOrcamento(numero);

        fluxo.saga(numero)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sagaId").value(saga(numero).getId().toString()))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_DECISAO"))
                .andExpect(jsonPath("$.passoAtual").value("DECISAO"))
                .andExpect(jsonPath("$.prazoExpiraEm").exists())
                .andExpect(jsonPath("$.passos[0].tipo").value("INICIO"))
                .andExpect(jsonPath("$.passos[1].tipo").value("ACAO_REGISTRADA"));
    }

    @Test
    void consultaDeOrdemSemSagaResponde404() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        mvc.perform(get("/sagas/" + UUID.randomUUID()).with(FluxoOficinaHttp.ATENDENTE))
                .andExpect(status().isNotFound());
    }

    private AlteracaoStatusOrdemDeServico ultimaAlteracao(String numero) {
        List<AlteracaoStatusOrdemDeServico> alteracoes = historico.listarPorOrdem(fluxo.ordem(numero).getId());
        return alteracoes.get(alteracoes.size() - 1);
    }

    private void publicarEvento(String tipo, UUID ordemServicoId, Map<String, Object> payload) {
        Envelope envelope = fabrica.novo(tipo, null, ordemServicoId, payload);
        transacao.executeWithoutResult(s -> publicadorMensagens.publicar(Filas.OS_SAGA_REPLIES, envelope));
    }
}
