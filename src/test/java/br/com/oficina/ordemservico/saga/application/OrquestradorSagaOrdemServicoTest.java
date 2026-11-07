package br.com.oficina.ordemservico.saga.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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

import com.jayway.jsonpath.JsonPath;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;
import br.com.oficina.common.infrastructure.mensageria.SpringDataOutboxMensagemRepository;
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
import br.com.oficina.support.saga.BillingSimulado.PagamentoSimulado;
import br.com.oficina.support.saga.BillingSimulado.StatusOrcamento;
import br.com.oficina.support.saga.BillingSimulado.StatusPagamento;
import br.com.oficina.support.saga.CatalogoSimulado;
import br.com.oficina.support.saga.ExecutionSimulado.Etapa;
import br.com.oficina.support.saga.FluxoOficinaHttp;

/**
 * Saga ponta a ponta no OS: abertura pela API HTTP real, outbox, transporte em memoria, consumidor
 * idempotente, orquestrador e os participantes simulados - a Execution (fila, diagnostico, execucao,
 * estoque: {@code ExecutionSimulado}, com o mecanico) e o Billing ({@code BillingSimulado}), que
 * respondem como o contrato manda.
 */
@SpringBootTest
class OrquestradorSagaOrdemServicoTest {

    private static final LocalDateTime DEPOIS_DA_VALIDADE = LocalDateTime.now().plusDays(10);

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

    private List<String> detalhesDosPassos(String numero) {
        return sagas.listarPassos(saga(numero).getId()).stream().map(RegistroPassoSaga::getDetalhe).toList();
    }

    private Etapa etapaNaExecution(String numero) {
        return fluxo.execution().etapa(fluxo.ordem(numero).getId());
    }

    /** OS aberta, diagnosticada pelo mecanico (um servico de R$ 200,00, sem pecas) e com orcamento gerado pela Saga. */
    private String diagnosticada(String placa) throws Exception {
        String numero = fluxo.abrirOrdem(placa);
        fluxo.diagnosticar(numero, "Troca de oleo");
        return numero;
    }

    /** O mesmo, com uma peca no diagnostico. */
    private String diagnosticadaComPeca(String placa, String pecaId, int quantidade) throws Exception {
        String numero = fluxo.abrirOrdem(placa);
        fluxo.diagnosticarComPeca(numero, "Troca do filtro de oleo", pecaId, quantidade);
        return numero;
    }

    /** Orcamento aprovado e pecas reservadas: a OS espera o pagamento. Devolve o numero do orcamento. */
    private String ateAguardarPagamento(String numero) {
        String orcamento = fluxo.enviarParaOrcamento(numero);
        fluxo.clienteAprova(orcamento);
        fluxo.processarMensagens();
        return orcamento;
    }

    private PagamentoSimulado pagamentoDaOrdem(String numero) {
        return fluxo.pagamento(fluxo.orcamento(numero).orElseThrow().getNumeroOrcamento()).orElseThrow();
    }

    private CatalogoSimulado.Peca peca(String pecaId) {
        return fluxo.peca(pecaId);
    }

    // ------------------------------------------------------------------ passo 1: fila e diagnostico

    @Test
    void aberturaDaOrdemIniciaASagaEColocaAOsNaFilaDeDiagnosticoDaExecution() throws Exception {
        String numero = fluxo.abrirOrdemSemEntregarMensagens("SAG1A11");

        SagaOrdemServico saga = saga(numero);
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga.getPrazoExpiraEm()).as("vigia a resposta da fila").isNotNull();
        assertThat(tiposDosPassos(numero)).containsExactly(RegistroPassoSaga.Tipo.INICIO, RegistroPassoSaga.Tipo.COMANDO_ENVIADO);
        String comando = outbox.findAll().stream()
                .filter(m -> MensagensSaga.ENFILEIRAR_DIAGNOSTICO.equals(m.getTipo())).findFirst().orElseThrow().getPayload();
        assertThat(JsonPath.<String>read(comando, "$.payload.numeroOrdemServico")).isEqualTo(numero);
        assertThat(JsonPath.<String>read(comando, "$.payload.veiculo.placa")).isEqualTo("SAG1A11");
        assertThat(JsonPath.<String>read(comando, "$.payload.abertaEm")).endsWith("Z");

        fluxo.processarMensagens();

        assertThat(etapaNaExecution(numero)).isEqualTo(Etapa.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga(numero).getPrazoExpiraEm()).as("o diagnostico depende do mecanico: sem prazo").isNull();
        assertThat(tiposDosPassos(numero)).containsExactly(
                RegistroPassoSaga.Tipo.INICIO, RegistroPassoSaga.Tipo.COMANDO_ENVIADO, RegistroPassoSaga.Tipo.EVENTO_RECEBIDO);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.OS_ABERTA);
    }

    @Test
    void previsaoDoAtendenteVaiParaAFilaComoPontoDePartidaDoMecanico() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = fluxo.abrirOrdemComPrevisao("SAG1A22", "Revisao dos 10 mil", "150.00", pecaId, 1);

        var execucao = fluxo.execution().execucaoDaOrdem(fluxo.ordem(numero).getId()).orElseThrow();
        assertThat(execucao.getServicosPrevistos()).singleElement()
                .satisfies(servico -> assertThat(servico).containsEntry("descricao", "Revisao dos 10 mil").containsEntry("valorMaoDeObra", "150.00"));
        assertThat(execucao.getPecasPrevistas()).singleElement()
                .satisfies(peca -> assertThat(peca).containsEntry("pecaInsumoId", pecaId).containsEntry("quantidade", 1));
    }

    @Test
    void mecanicoIniciaODiagnosticoEAOrdemPassaAFicarEmDiagnostico() throws Exception {
        String numero = fluxo.abrirOrdem("SAG1B11");

        fluxo.iniciarDiagnostico(numero);

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(ultimaAlteracao(numero).origem()).isEqualTo(OrigemAlteracaoStatus.SAGA);
        assertThat(detalhesDosPassos(numero)).anySatisfy(d -> assertThat(d).startsWith(MensagensSaga.DIAGNOSTICO_INICIADO));
    }

    @Test
    void diagnosticoConcluidoGeraOOrcamentoSozinhoSemNinguemEnviarParaOrcamento() throws Exception {
        String numero = fluxo.abrirOrdem("SAG2A22");
        fluxo.iniciarDiagnostico(numero);

        fluxo.execution().concluirDiagnostico(fluxo.ordem(numero).getId(), "Oleo vencido", Map.of("Troca de oleo", "200.00"), Map.of());
        fluxo.processarUmCiclo(); // entrega o DiagnosticoConcluido; a resposta do Billing ainda nao

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.GERANDO_ORCAMENTO);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO);
        assertThat(fluxo.ordem(numero).getServicos()).singleElement().satisfies(servico -> {
            assertThat(servico.getDescricao()).isEqualTo("Troca de oleo");
            assertThat(servico.getValorMaoDeObra()).isEqualByComparingTo("200.00");
        });
        assertThat(fluxo.orcamento(numero)).as("o OS nao cria o orcamento").isEmpty();
        String gerarOrcamento = outbox.findAll().stream()
                .filter(m -> MensagensSaga.GERAR_ORCAMENTO.equals(m.getTipo())).findFirst().orElseThrow().getPayload();
        // Snapshot do diagnostico no formato do contrato (docs/contratos): dinheiro com duas casas, o tipo do
        // cliente (PF/PJ) sempre presente e os opcionais que o OS nao informa mais fora do JSON.
        assertThat(JsonPath.<String>read(gerarOrcamento, "$.payload.numeroOrdemServico")).isEqualTo(numero);
        assertThat(JsonPath.<String>read(gerarOrcamento, "$.payload.cliente.documento")).isEqualTo("52998224725");
        assertThat(JsonPath.<String>read(gerarOrcamento, "$.payload.cliente.tipo")).isEqualTo("PF");
        assertThat(JsonPath.<String>read(gerarOrcamento, "$.payload.descricaoDiagnostico")).isEqualTo("Oleo vencido");
        assertThat(JsonPath.<String>read(gerarOrcamento, "$.payload.servicos[0].descricao")).isEqualTo("Troca de oleo");
        assertThat(JsonPath.<String>read(gerarOrcamento, "$.payload.servicos[0].valorMaoDeObra")).isEqualTo("200.00");
        assertThat(JsonPath.<Map<String, Object>>read(gerarOrcamento, "$.payload"))
                .doesNotContainKeys("valorMaoDeObra", "desconto", "validade", "observacoes");

        fluxo.processarMensagens();

        assertThat(fluxo.orcamento(numero)).hasValueSatisfying(o -> {
            assertThat(o.getSagaId()).isEqualTo(saga(numero).getId());
            assertThat(o.getValorTotal()).as("soma da mao de obra dos servicos").isEqualByComparingTo("200.00");
        });
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
        assertThat(saga(numero).getPrazoExpiraEm()).as("prazo de decisao = validade do orcamento (7 dias)")
                .isAfter(LocalDateTime.now().plusDays(6)).isBefore(LocalDateTime.now().plusDays(8));
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_APROVACAO);
        assertThat(ultimaAlteracao(numero).origem()).isEqualTo(OrigemAlteracaoStatus.SAGA);
    }

    @Test
    void diagnosticoConcluidoSemTerSidoIniciadoNaoTravaAOrcamento() throws Exception {
        String numero = fluxo.abrirOrdem("SAG2B22");
        // O DiagnosticoIniciado nunca chegou ao OS (ou chegou depois): a conclusao anda os dois passos.
        fluxo.execution().iniciarDiagnostico(fluxo.ordem(numero).getId());
        fluxo.perderMensagensPendentes(MensagensSaga.DIAGNOSTICO_INICIADO);

        fluxo.concluirDiagnostico(numero, "Revisao", null, 0);

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_APROVACAO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
    }

    // ------------------------------------------------------------------ passos 3 a 6: decisao, reserva, pagamento, execucao

    @Test
    void caminhoFelizReservaAsPecasCobraEnfileiraAExecucaoEBaixaOEstoqueNaConclusao() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = diagnosticadaComPeca("SAG3A33", pecaId, 2);
        String orcamento = fluxo.enviarParaOrcamento(numero);

        fluxo.clienteAprova(orcamento);
        assertThat(fluxo.ordem(numero).getStatus()).as("so depois do outbox").isEqualTo(StatusOrdemDeServico.AGUARDANDO_APROVACAO);
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_PAGAMENTO);
        assertThat(peca(pecaId).getReservada()).isEqualTo(2);
        assertThat(peca(pecaId).getDisponivel()).isEqualTo(3);
        SagaOrdemServico aguardandoPagamento = saga(numero);
        PagamentoSimulado cobranca = pagamentoDaOrdem(numero);
        assertThat(aguardandoPagamento.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_PAGAMENTO);
        assertThat(aguardandoPagamento.getPagamentoId()).isEqualTo(cobranca.getId());
        assertThat(aguardandoPagamento.getLinkPagamento()).isEqualTo(cobranca.getLinkPagamento()).contains(cobranca.getId().toString());
        assertThat(cobranca.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(cobranca.getValor()).isEqualByComparingTo("360.00");
        assertThat(aguardandoPagamento.getPrazoExpiraEm()).as("prazo do pagamento + margem")
                .isCloseTo(LocalDateTime.ofInstant(cobranca.getExpiraEm(), ZoneId.systemDefault()).plusMinutes(10), within(1, ChronoUnit.MILLIS));

        fluxo.simularPagamento(orcamento, "APROVADO");
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_EXECUCAO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_EXECUCAO);
        assertThat(saga(numero).getPrazoExpiraEm()).as("ExecucaoEnfileirada chegou: a execucao e do mecanico").isNull();
        assertThat(etapaNaExecution(numero)).isEqualTo(Etapa.AGUARDANDO_EXECUCAO);
        AlteracaoStatusOrdemDeServico ultima = ultimaAlteracao(numero);
        assertThat(ultima.origem()).isEqualTo(OrigemAlteracaoStatus.SAGA);
        assertThat(ultima.sagaId()).isEqualTo(saga(numero).getId());
        assertThat(detalhesDosPassos(numero)).anySatisfy(d -> assertThat(d).contains("OrcamentoAprovado").contains("CLIENTE"));
        assertThat(detalhesDosPassos(numero)).contains(MensagensSaga.ENFILEIRAR_EXECUCAO);

        fluxo.iniciarExecucao(numero);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.EM_EXECUCAO);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.SERVICO_EM_ANDAMENTO);
        fluxo.concluirExecucao(numero);

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.CONCLUIDA);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.OS_FINALIZADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.SERVICO_CONCLUIDO);
        assertThat(peca(pecaId).getEstoque()).as("a Execution baixou o que estava reservado").isEqualTo(3);
        assertThat(peca(pecaId).getReservada()).isZero();
    }

    @Test
    void estoqueInsuficienteReverteAAprovacaoEOClienteDecideDeNovo() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 1);
        String numero = diagnosticadaComPeca("SAG4A44", pecaId, 2);
        String orcamento = fluxo.enviarParaOrcamento(numero);

        fluxo.clienteAprova(orcamento);
        fluxo.processarMensagens();

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
        assertThat(saga(numero).getMotivo()).contains("Estoque insuficiente").contains(pecaId);
        assertThat(saga(numero).getPrazoExpiraEm()).as("prazo de decisao novo (padrao de 7 dias)")
                .isAfter(LocalDateTime.now().plusDays(6));
        assertThat(fluxo.orcamento(numero).orElseThrow().getStatus()).isEqualTo(StatusOrcamento.AGUARDANDO_APROVACAO);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_APROVACAO);
        assertThat(peca(pecaId).getReservada()).as("tudo ou nada").isZero();
        assertThat(outbox.findAll()).extracting(m -> m.getTipo())
                .contains(MensagensSaga.RESERVA_FALHOU, MensagensSaga.REVERTER_APROVACAO, MensagensSaga.APROVACAO_REVERTIDA);

        // Chegou mercadoria: a nova aprovacao reserva.
        fluxo.catalogo().definirEstoque(pecaId, 10);
        fluxo.clienteAprova(orcamento);
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_PAGAMENTO);
        assertThat(peca(pecaId).getReservada()).isEqualTo(2);
    }

    @Test
    void pagamentoRecusadoLiberaAsPecasCancelaOOrcamentoAExecucaoEAOrdem() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = diagnosticadaComPeca("SAG5A55", pecaId, 2);
        String orcamento = ateAguardarPagamento(numero);

        fluxo.simularPagamento(orcamento, "RECUSADO");
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.CANCELADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.PAGAMENTO_RECUSADO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSADA);
        assertThat(fluxo.orcamento(numero).orElseThrow().getStatus()).isEqualTo(StatusOrcamento.CANCELADO);
        assertThat(pagamentoDaOrdem(numero).getStatus()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(etapaNaExecution(numero)).as("a execucao parada em diagnostico concluido e cancelada").isEqualTo(Etapa.CANCELADA);
        assertThat(peca(pecaId).getReservada()).isZero();
        assertThat(peca(pecaId).getEstoque()).isEqualTo(5);
        assertThat(outbox.findAll()).extracting(m -> m.getTipo())
                .contains(MensagensSaga.CANCELAR_EXECUCAO, MensagensSaga.EXECUCAO_CANCELADA);
    }

    @Test
    void pagamentoExpiradoNoBillingCancelaAOrdem() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = diagnosticadaComPeca("SAG6A66", pecaId, 1);
        String orcamento = ateAguardarPagamento(numero);

        fluxo.expirarPagamento(orcamento);
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.CANCELADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.PAGAMENTO_EXPIRADO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSADA);
        assertThat(pagamentoDaOrdem(numero).getStatus()).isEqualTo(StatusPagamento.EXPIRADO);
        assertThat(etapaNaExecution(numero)).isEqualTo(Etapa.CANCELADA);
        assertThat(peca(pecaId).getReservada()).isZero();
    }

    @Test
    void semNoticiaDoBillingAteOPrazoMaisMargemAOrdemEhCanceladaPelaRedeDeSeguranca() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = diagnosticadaComPeca("SAG7A77", pecaId, 1);
        ateAguardarPagamento(numero);

        assertThat(orquestrador.processarPrazosVencidos(LocalDateTime.now().plusHours(24))).as("dentro da margem").isZero();
        assertThat(orquestrador.processarPrazosVencidos(LocalDateTime.now().plusDays(2))).isEqualTo(1);

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.CANCELADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.PAGAMENTO_EXPIRADO);
        assertThat(ultimaAlteracao(numero).origem()).isEqualTo(OrigemAlteracaoStatus.TIMEOUT);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSANDO);

        fluxo.processarMensagens();

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSADA);
        assertThat(pagamentoDaOrdem(numero).getStatus()).as("cobranca pendente sai junto com o orcamento")
                .isEqualTo(StatusPagamento.CANCELADO);
        assertThat(peca(pecaId).getReservada()).isZero();
    }

    @Test
    void execucaoAbortadaEstornaOPagamentoELiberaAsPecas() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = diagnosticadaComPeca("SAG8A88", pecaId, 2);
        String orcamento = ateAguardarPagamento(numero);
        fluxo.simularPagamento(orcamento, "APROVADO");
        fluxo.processarMensagens();
        fluxo.iniciarExecucao(numero);

        fluxo.execution().abortarExecucao(fluxo.ordem(numero).getId(), "Peca errada");
        fluxo.processarUmCiclo(); // ExecucaoFalhou chega; estorno e liberacao ainda pendentes
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.CANCELADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.FALHA_EXECUCAO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSADA);
        assertThat(pagamentoDaOrdem(numero).getStatus()).isEqualTo(StatusPagamento.ESTORNADO);
        assertThat(pagamentoDaOrdem(numero).getMpRefundId()).startsWith("SIM-REFUND-");
        assertThat(fluxo.orcamento(numero).orElseThrow().getStatus()).isEqualTo(StatusOrcamento.CANCELADO);
        assertThat(peca(pecaId).getReservada()).isZero();
        assertThat(peca(pecaId).getEstoque()).isEqualTo(5);
        assertThat(detalhesDosPassos(numero)).anySatisfy(d -> assertThat(d).contains("Peca errada"));
    }

    // ------------------------------------------------------------------ orcamento que falha: ReabrirDiagnostico

    @Test
    void orcamentoComPecaQueSumiuDoCatalogoReabreODiagnosticoNaExecution() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = fluxo.abrirOrdem("SAG9A99");
        fluxo.iniciarDiagnostico(numero);
        fluxo.catalogo().remover(pecaId); // sai do catalogo entre o diagnostico e o orcamento

        fluxo.concluirDiagnostico(numero, "Troca do filtro", pecaId, 1);

        assertThat(fluxo.orcamento(numero)).isEmpty();
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga(numero).getMotivo()).contains(pecaId);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO);
        assertThat(fluxo.ordem(numero).getPecasPrevistas()).as("pecas saem").isEmpty();
        assertThat(fluxo.ordem(numero).getServicos()).as("servicos ficam").isNotEmpty();
        assertThat(etapaNaExecution(numero)).as("de volta a fila do mecanico").isEqualTo(Etapa.AGUARDANDO_DIAGNOSTICO);
        assertThat(detalhesDosPassos(numero)).anySatisfy(d -> assertThat(d).contains("diagnostico sera reaberto"));
        assertThat(detalhesDosPassos(numero)).anySatisfy(d -> assertThat(d).startsWith(MensagensSaga.DIAGNOSTICO_REABERTO));

        // O mecanico refaz o diagnostico sem a peca e o orcamento nasce.
        fluxo.iniciarDiagnostico(numero);
        fluxo.concluirDiagnostico(numero, "Troca do filtro", null, 0);

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_APROVACAO);
        assertThat(fluxo.orcamento(numero)).isPresent();
    }

    @Test
    void comandoSemRespostaEhReenviadoEDepoisTratadoComoFalhaReabrindoODiagnostico() throws Exception {
        String numero = fluxo.abrirOrdem("SAG1D11");
        fluxo.iniciarDiagnostico(numero);
        fluxo.execution().concluirDiagnostico(fluxo.ordem(numero).getId(), "Oleo", Map.of("Troca de oleo", "200.00"), Map.of());
        fluxo.processarUmCiclo(); // DiagnosticoConcluido -> GerarOrcamento no outbox
        fluxo.perderMensagensPendentes(MensagensSaga.GERAR_ORCAMENTO);
        LocalDateTime agora = LocalDateTime.now();

        for (int reenvio = 1; reenvio <= 3; reenvio++) {
            agora = agora.plusMinutes(6);
            assertThat(orquestrador.processarPrazosVencidos(agora)).isEqualTo(1);
            assertThat(saga(numero).getTentativasPasso()).isEqualTo(reenvio);
        }
        assertThat(outbox.findAll()).filteredOn(m -> MensagensSaga.GERAR_ORCAMENTO.equals(m.getTipo()))
                .hasSize(4)
                .extracting(m -> m.getId()).doesNotHaveDuplicates();
        assertThat(tiposDosPassos(numero)).filteredOn(t -> t == RegistroPassoSaga.Tipo.COMANDO_REENVIADO).hasSize(3);

        orquestrador.processarPrazosVencidos(agora.plusMinutes(6));

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        assertThat(saga(numero).getMotivo()).contains("nao respondeu");
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO);
        assertThat(ultimaAlteracao(numero).origem()).isEqualTo(OrigemAlteracaoStatus.TIMEOUT);

        // Os reenvios ainda pendentes chegam atrasados: o Billing gera um orcamento so e responde a todos,
        // mas a Saga ja desistiu e ignora; a Execution confirma a reabertura.
        fluxo.processarMensagens();
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(tiposDosPassos(numero)).contains(RegistroPassoSaga.Tipo.EVENTO_IGNORADO);
        assertThat(etapaNaExecution(numero)).isEqualTo(Etapa.AGUARDANDO_DIAGNOSTICO);

        fluxo.iniciarDiagnostico(numero);
        fluxo.concluirDiagnostico(numero, "Oleo", null, 0);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
    }

    // ------------------------------------------------------------------ fila sem resposta da Execution

    @Test
    void filaDeDiagnosticoSemConfirmacaoEhReenviadaEDepoisExigeAcaoManual() throws Exception {
        String numero = fluxo.abrirOrdemSemEntregarMensagens("SAG1N11");
        fluxo.perderMensagensPendentes(MensagensSaga.ENFILEIRAR_DIAGNOSTICO);
        LocalDateTime agora = LocalDateTime.now();

        for (int reenvio = 1; reenvio <= 3; reenvio++) {
            agora = agora.plusMinutes(6);
            assertThat(orquestrador.processarPrazosVencidos(agora)).isEqualTo(1);
            fluxo.perderMensagensPendentes(MensagensSaga.ENFILEIRAR_DIAGNOSTICO);
        }
        assertThat(outbox.findAll()).filteredOn(m -> MensagensSaga.ENFILEIRAR_DIAGNOSTICO.equals(m.getTipo())).hasSize(4);

        orquestrador.processarPrazosVencidos(agora.plusMinutes(6));

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.FALHA_COMPENSACAO);
        assertThat(saga(numero).getMotivo()).contains("nao confirmou a fila");
        assertThat(orquestrador.processarPrazosVencidos(agora.plusDays(1))).as("estado final nao vence").isZero();
    }

    @Test
    void filaDeExecucaoSemConfirmacaoEhReenviadaEAExecutionRespondeSemDuplicar() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = diagnosticadaComPeca("SAG1O11", pecaId, 1);
        String orcamento = ateAguardarPagamento(numero);
        fluxo.simularPagamento(orcamento, "APROVADO");
        fluxo.processarUmCiclo(); // PagamentoConfirmado -> EnfileirarExecucao no outbox
        fluxo.perderMensagensPendentes(MensagensSaga.ENFILEIRAR_EXECUCAO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_EXECUCAO);
        assertThat(saga(numero).getPrazoExpiraEm()).isNotNull();

        assertThat(orquestrador.processarPrazosVencidos(LocalDateTime.now().plusMinutes(6))).isEqualTo(1);
        fluxo.processarMensagens();

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_EXECUCAO);
        assertThat(saga(numero).getPrazoExpiraEm()).as("ExecucaoEnfileirada do reenvio confirmou a fila").isNull();
        assertThat(etapaNaExecution(numero)).isEqualTo(Etapa.AGUARDANDO_EXECUCAO);
        assertThat(tiposDosPassos(numero)).contains(RegistroPassoSaga.Tipo.COMANDO_REENVIADO);
    }

    @Test
    void reservaSemRespostaLiberaOQueHouverEReverteAAprovacao() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = diagnosticadaComPeca("SAG1E11", pecaId, 1);
        String orcamento = fluxo.enviarParaOrcamento(numero);
        fluxo.clienteAprova(orcamento);
        fluxo.processarUmCiclo();
        assertThat(fluxo.perderMensagensPendentes(MensagensSaga.RESERVAR_PECAS)).isEqualTo(1);
        LocalDateTime agora = LocalDateTime.now();

        for (int i = 1; i <= 3; i++) {
            agora = agora.plusMinutes(6);
            orquestrador.processarPrazosVencidos(agora);
            fluxo.perderMensagensPendentes(MensagensSaga.RESERVAR_PECAS);
        }
        orquestrador.processarPrazosVencidos(agora.plusMinutes(6));

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.REVERTENDO_APROVACAO);
        assertThat(saga(numero).getCompensacoesPendentes())
                .containsExactlyInAnyOrder(MensagensSaga.APROVACAO_REVERTIDA, MensagensSaga.RESERVA_LIBERADA);

        fluxo.processarMensagens();

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
        assertThat(fluxo.orcamento(numero).orElseThrow().getStatus()).isEqualTo(StatusOrcamento.AGUARDANDO_APROVACAO);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_APROVACAO);
        assertThat(peca(pecaId).getReservada()).isZero();
    }

    // ------------------------------------------------------------------ rejeicao e prazo de decisao

    @Test
    void rejeicaoEncerraAOrdemCancelaAExecucaoEASagaSemExecucao() throws Exception {
        String numero = diagnosticada("SAG1F11");
        String orcamento = fluxo.enviarParaOrcamento(numero);

        fluxo.clienteRejeita(orcamento);
        fluxo.processarUmCiclo(); // OrcamentoRejeitado chega; o cancelamento da execucao ainda nao foi confirmado
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        assertThat(fluxo.ordem(numero).getStatus()).as("a OS encerra na hora").isEqualTo(StatusOrdemDeServico.OS_FINALIZADA);
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.OS_FINALIZADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.ORCAMENTO_RECUSADO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(etapaNaExecution(numero)).isEqualTo(Etapa.CANCELADA);
        assertThat(detalhesDosPassos(numero)).contains(MensagensSaga.CANCELAR_EXECUCAO)
                .anySatisfy(d -> assertThat(d).startsWith(MensagensSaga.EXECUCAO_CANCELADA));
    }

    @Test
    void prazoDeDecisaoVencidoCancelaOrcamentoEExecucaoPorCompensacaoEEncerraAOrdem() throws Exception {
        String numero = diagnosticada("SAG1G11");
        fluxo.enviarParaOrcamento(numero);

        assertThat(orquestrador.processarPrazosVencidos(LocalDateTime.now().plusDays(1))).as("ainda no prazo").isZero();
        assertThat(orquestrador.processarPrazosVencidos(DEPOIS_DA_VALIDADE)).isEqualTo(1);

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.OS_FINALIZADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.ORCAMENTO_EXPIRADO);
        assertThat(ultimaAlteracao(numero).origem()).isEqualTo(OrigemAlteracaoStatus.TIMEOUT);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        assertThat(saga(numero).getCompensacoesPendentes())
                .containsExactlyInAnyOrder(MensagensSaga.ORCAMENTO_CANCELADO, MensagensSaga.EXECUCAO_CANCELADA);
        assertThat(outbox.findAll()).extracting(m -> m.getTipo())
                .contains(MensagensSaga.CANCELAR_ORCAMENTO, MensagensSaga.CANCELAR_EXECUCAO);

        fluxo.processarMensagens();

        assertThat(fluxo.orcamento(numero).orElseThrow().getStatus()).isEqualTo(StatusOrcamento.CANCELADO);
        assertThat(etapaNaExecution(numero)).isEqualTo(Etapa.CANCELADA);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(saga(numero).getCompensacoesPendentes()).isEmpty();
        assertThat(tiposDosPassos(numero)).contains(
                RegistroPassoSaga.Tipo.TIMEOUT, RegistroPassoSaga.Tipo.COMANDO_ENVIADO, RegistroPassoSaga.Tipo.EVENTO_RECEBIDO);
    }

    @Test
    void confirmacoesPerdidasSaoPedidasDeNovoEOsParticipantesRepetemAResposta() throws Exception {
        String numero = diagnosticada("SAG1H11");
        fluxo.enviarParaOrcamento(numero);
        orquestrador.processarPrazosVencidos(DEPOIS_DA_VALIDADE);
        // As ordens de cancelamento se perdem no caminho: nenhum participante as recebeu.
        fluxo.perderMensagensPendentes(MensagensSaga.CANCELAR_ORCAMENTO);
        fluxo.perderMensagensPendentes(MensagensSaga.CANCELAR_EXECUCAO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSANDO);

        assertThat(orquestrador.processarPrazosVencidos(DEPOIS_DA_VALIDADE.plusMinutes(10))).isEqualTo(1);
        assertThat(saga(numero).getTentativasPasso()).isEqualTo(1);
        fluxo.processarMensagens();

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(etapaNaExecution(numero)).isEqualTo(Etapa.CANCELADA);
        assertThat(fluxo.orcamento(numero).orElseThrow().getStatus()).isEqualTo(StatusOrcamento.CANCELADO);
    }

    @Test
    void semConfirmacaoAposTodasAsTentativasASagaFicaEmFalhaDeCompensacao() throws Exception {
        String numero = diagnosticada("SAG1I11");
        fluxo.enviarParaOrcamento(numero);
        LocalDateTime agora = DEPOIS_DA_VALIDADE;
        orquestrador.processarPrazosVencidos(agora);

        // Os participantes nunca respondem: o outbox nao e processado.
        for (int i = 1; i <= 4; i++) {
            agora = agora.plusMinutes(10);
            orquestrador.processarPrazosVencidos(agora);
        }

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.FALHA_COMPENSACAO);
        assertThat(outbox.findAll()).filteredOn(m -> MensagensSaga.CANCELAR_ORCAMENTO.equals(m.getTipo())).hasSize(4);
        assertThat(outbox.findAll()).filteredOn(m -> MensagensSaga.CANCELAR_EXECUCAO.equals(m.getTipo())).hasSize(4);
        assertThat(orquestrador.processarPrazosVencidos(agora.plusDays(1))).as("estado final nao vence").isZero();
    }

    // ------------------------------------------------------------------ robustez

    @Test
    void eventoQueNaoCabeNoEstadoAtualEhIgnoradoEConfirmado() throws Exception {
        String numero = diagnosticada("SAG1J11");
        String orcamento = fluxo.enviarParaOrcamento(numero);
        fluxo.clienteRejeita(orcamento);
        fluxo.processarMensagens();
        UUID orcamentoId = fluxo.orcamento(numero).orElseThrow().getId();

        // Uma aprovacao atrasada (ex.: reenviada por um sistema externo) depois da rejeicao.
        publicarEvento(MensagensSaga.ORCAMENTO_APROVADO, fluxo.ordem(numero).getId(),
                Map.of("orcamentoId", orcamentoId.toString(), "canal", "INTEGRACAO", "decididoEm", "2026-10-15T13:00:00Z"));
        // E um pagamento de uma cobranca que nao existe.
        publicarEvento(MensagensSaga.PAGAMENTO_CONFIRMADO, fluxo.ordem(numero).getId(),
                Map.of("pagamentoId", UUID.randomUUID().toString(), "valor", "10.00", "aprovadoEm", "2026-10-15T13:00:00Z"));
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.OS_FINALIZADA);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(tiposDosPassos(numero)).filteredOn(t -> t == RegistroPassoSaga.Tipo.EVENTO_IGNORADO).hasSize(2);
        assertThat(outbox.findAll()).allSatisfy(m -> assertThat(m.getPublicadaEm()).isNotNull());
    }

    @Test
    void ordemSemSagaEhAdotadaQuandoOPrimeiroEventoChega() throws Exception {
        String numero = diagnosticada("SAG1K11");
        String orcamento = fluxo.enviarParaOrcamento(numero);
        transacao.executeWithoutResult(s -> springDataSagas.deleteAll());

        fluxo.clienteAprova(orcamento);
        fluxo.processarMensagens();

        assertThat(tiposDosPassos(numero)).startsWith(RegistroPassoSaga.Tipo.ADOCAO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_PAGAMENTO);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void eventosDaExecutionForaDoEstadoEsperadoSaoRegistradosComoIgnorados() throws Exception {
        String numero = fluxo.abrirOrdem("SAG1L11");
        UUID ordemId = fluxo.ordem(numero).getId();
        Map<String, String> mecanico = Map.of("id", "m@oficina.com", "nome", "Beto");

        publicarEvento(MensagensSaga.EXECUCAO_ENFILEIRADA, ordemId, Map.of("posicaoNaFila", 1));
        publicarEvento(MensagensSaga.EXECUCAO_INICIADA, ordemId, Map.of("mecanico", mecanico, "iniciadaEm", "2026-10-17T12:00:00Z"));
        publicarEvento(MensagensSaga.EXECUCAO_CONCLUIDA, ordemId,
                Map.of("mecanico", mecanico, "pecasConsumidas", List.of(), "concluidaEm", "2026-10-17T12:00:00Z"));
        publicarEvento(MensagensSaga.EXECUCAO_FALHOU, ordemId,
                Map.of("mecanico", mecanico, "motivo", "sem execucao", "abortadaEm", "2026-10-17T12:00:00Z"));
        publicarEvento(MensagensSaga.DIAGNOSTICO_ENFILEIRADO, ordemId, Map.of("posicaoNaFila", 1)); // repetido: fila ja confirmada
        fluxo.processarMensagens();

        assertThat(tiposDosPassos(numero)).filteredOn(t -> t == RegistroPassoSaga.Tipo.EVENTO_IGNORADO).hasSize(5);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.OS_ABERTA);
    }

    @Test
    void consultaDaSagaMostraEstadoLinkDePagamentoEHistoria() throws Exception {
        String numero = diagnosticada("SAG1M11");
        ateAguardarPagamento(numero);

        fluxo.saga(numero)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sagaId").value(saga(numero).getId().toString()))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.passoAtual").value("PAGAMENTO"))
                .andExpect(jsonPath("$.pagamentoId").value(saga(numero).getPagamentoId().toString()))
                .andExpect(jsonPath("$.linkPagamento").value(saga(numero).getLinkPagamento()))
                .andExpect(jsonPath("$.prazoExpiraEm").exists())
                .andExpect(jsonPath("$.passos[0].tipo").value("INICIO"))
                .andExpect(jsonPath("$.passos[1].tipo").value("COMANDO_ENVIADO"));
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
