package br.com.oficina.ordemservico.saga.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import com.jayway.jsonpath.JsonPath;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.common.infrastructure.mensageria.SpringDataOutboxMensagemRepository;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
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
import br.com.oficina.pagamento.application.ResultadoPagamentoService;
import br.com.oficina.pagamento.domain.model.Pagamento;
import br.com.oficina.pagamento.domain.model.StatusPagamento;
import br.com.oficina.pagamento.domain.repository.PagamentoRepository;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.infrastructure.persistence.SpringDataPecaInsumoRepository;
import br.com.oficina.support.saga.FluxoOficinaHttp;

/**
 * Saga ponta a ponta dentro do monolito: APIs HTTP reais, outbox, transporte em memoria, consumidor
 * idempotente, orquestrador e os participantes (orcamento e pagamento - futuro Billing; estoque -
 * futura Execution).
 */
@SpringBootTest
class OrquestradorSagaOrdemServicoTest {

    private static final LocalDateTime VALIDADE = LocalDateTime.of(2030, 1, 1, 0, 0);

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

    @Autowired
    private PagamentoRepository pagamentos;

    @Autowired
    private ResultadoPagamentoService resultadoPagamento;

    @Autowired
    private SpringDataPecaInsumoRepository springDataPecas;

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

    private String diagnosticada(String placa) throws Exception {
        String numero = fluxo.abrirOrdem(placa);
        fluxo.diagnosticar(numero, "Troca de oleo");
        return numero;
    }

    /** OS com uma peca prevista ({@code quantidade} de um estoque de {@code estoque}). */
    private String diagnosticadaComPeca(String placa, String pecaId, int quantidade) throws Exception {
        String numero = fluxo.abrirOrdem(placa);
        fluxo.diagnosticarComPeca(numero, "Troca do filtro de oleo", pecaId, quantidade);
        return numero;
    }

    /** Orcamento aprovado e pecas reservadas: a OS espera o pagamento. Devolve o numero do orcamento. */
    private String ateAguardarPagamento(String numero) throws Exception {
        String orcamento = fluxo.enviarParaOrcamento(numero);
        fluxo.clienteAprova(orcamento);
        fluxo.processarMensagens();
        return orcamento;
    }

    private Pagamento pagamentoDaOrdem(String numero) {
        return pagamentos.buscarPorOrcamento(fluxo.orcamento(numero).orElseThrow().getId()).get(0);
    }

    private PecaInsumo peca(String pecaId) {
        return springDataPecas.findById(pecaId).orElseThrow().toDomain();
    }

    @Test
    void aberturaDaOrdemIniciaASaga() throws Exception {
        String numero = fluxo.abrirOrdem("SAG1A11");

        SagaOrdemServico saga = saga(numero);
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(tiposDosPassos(numero)).containsExactly(RegistroPassoSaga.Tipo.INICIO);
    }

    @Test
    void envioParaOrcamentoEhAssincronoEAOrdemSoMudaQuandoOBillingResponde() throws Exception {
        String numero = diagnosticada("SAG2A22");

        fluxo.solicitarOrcamento(numero);

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.GERANDO_ORCAMENTO);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO);
        assertThat(fluxo.orcamento(numero)).as("o OS nao cria mais o orcamento").isEmpty();
        String gerarOrcamento = outbox.findAll().stream()
                .filter(m -> MensagensSaga.GERAR_ORCAMENTO.equals(m.getTipo())).findFirst().orElseThrow().getPayload();
        // Snapshot do diagnostico no formato do contrato (docs/contratos): dinheiro com duas casas,
        // opcionais nulos fora do JSON.
        assertThat(JsonPath.<String>read(gerarOrcamento, "$.payload.numeroOrdemServico")).isEqualTo(numero);
        assertThat(JsonPath.<String>read(gerarOrcamento, "$.payload.cliente.documento")).isEqualTo("52998224725");
        assertThat(JsonPath.<String>read(gerarOrcamento, "$.payload.servicos[0].descricao")).isEqualTo("Troca de oleo");
        assertThat(JsonPath.<String>read(gerarOrcamento, "$.payload.valorMaoDeObra")).isEqualTo("200.00");
        assertThat(JsonPath.<String>read(gerarOrcamento, "$.payload.desconto")).isEqualTo("0.00");
        assertThat(JsonPath.<String>read(gerarOrcamento, "$.payload.validade")).endsWith("Z");
        assertThat(JsonPath.<Map<String, Object>>read(gerarOrcamento, "$.payload")).doesNotContainKey("observacoes");
        assertThat(JsonPath.<Map<String, Object>>read(gerarOrcamento, "$.payload.servicos[0]")).doesNotContainKey("valorMaoDeObra");

        fluxo.processarMensagens();

        assertThat(fluxo.orcamento(numero)).hasValueSatisfying(o -> {
            assertThat(o.getSagaId()).isEqualTo(saga(numero).getId());
            assertThat(o.getValorTotal()).isEqualByComparingTo("200.00");
        });
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
        assertThat(saga(numero).getPrazoExpiraEm()).as("prazo de decisao = validade do orcamento").isEqualTo(VALIDADE);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_APROVACAO);
        assertThat(ultimaAlteracao(numero).origem()).isEqualTo(OrigemAlteracaoStatus.SAGA);
    }

    @Test
    void orcamentoSoPodeSerSolicitadoUmaVezEnquantoOBillingNaoResponde() throws Exception {
        String numero = diagnosticada("SAG2B22");
        fluxo.solicitarOrcamento(numero);
        UUID ordemId = fluxo.ordem(numero).getId();
        PassosDaSaga.DadosOrcamento dados = new PassosDaSaga.DadosOrcamento(BigDecimal.TEN, null, null, null);

        assertThatThrownBy(() -> orquestrador.solicitarOrcamento(ordemId, dados))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("GERANDO_ORCAMENTO");
    }

    @Test
    void caminhoFelizReservaAsPecasCobraEBaixaOEstoqueNaConclusao() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = diagnosticadaComPeca("SAG3A33", pecaId, 2);
        String orcamento = fluxo.enviarParaOrcamento(numero);

        fluxo.clienteAprova(orcamento);
        assertThat(fluxo.ordem(numero).getStatus()).as("so depois do outbox").isEqualTo(StatusOrdemDeServico.AGUARDANDO_APROVACAO);
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_PAGAMENTO);
        assertThat(peca(pecaId).getQuantidadeReservada()).isEqualTo(2);
        assertThat(peca(pecaId).getQuantidadeDisponivel()).isEqualTo(3);
        SagaOrdemServico aguardandoPagamento = saga(numero);
        Pagamento cobranca = pagamentoDaOrdem(numero);
        assertThat(aguardandoPagamento.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_PAGAMENTO);
        assertThat(aguardandoPagamento.getPagamentoId()).isEqualTo(cobranca.getId());
        assertThat(aguardandoPagamento.getLinkPagamento()).isEqualTo(cobranca.getLinkPagamento()).contains(cobranca.getId().toString());
        assertThat(cobranca.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(cobranca.getValor()).isEqualByComparingTo("360.00");
        assertThat(aguardandoPagamento.getPrazoExpiraEm()).as("prazo do pagamento + margem")
                .isEqualTo(cobranca.getExpiraEm().plusMinutes(10));

        fluxo.simularPagamento(orcamento, "APROVADO").andExpect(status().isOk());
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_EXECUCAO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_EXECUCAO);
        AlteracaoStatusOrdemDeServico ultima = ultimaAlteracao(numero);
        assertThat(ultima.origem()).isEqualTo(OrigemAlteracaoStatus.SAGA);
        assertThat(ultima.sagaId()).isEqualTo(saga(numero).getId());
        assertThat(detalhesDosPassos(numero)).anySatisfy(d -> assertThat(d).contains("OrcamentoAprovado").contains("CLIENTE"));

        fluxo.acao(numero, "/execucao/iniciar");
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.EM_EXECUCAO);
        fluxo.acao(numero, "/servico/concluir");

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.CONCLUIDA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.SERVICO_CONCLUIDO);
        assertThat(peca(pecaId).getQuantidadeEstoque()).isEqualTo(3);
        assertThat(peca(pecaId).getQuantidadeReservada()).isZero();
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
        assertThat(peca(pecaId).getQuantidadeReservada()).as("tudo ou nada").isZero();
        assertThat(outbox.findAll()).extracting(m -> m.getTipo())
                .contains(MensagensSaga.RESERVA_FALHOU, MensagensSaga.REVERTER_APROVACAO, MensagensSaga.APROVACAO_REVERTIDA);

        // Chegou mercadoria: a nova aprovacao reserva.
        PecaInsumo reposta = peca(pecaId);
        springDataPecas.save(br.com.oficina.pecainsumo.infrastructure.persistence.PecaInsumoJpaEntity.fromDomain(new PecaInsumo(
                pecaId, reposta.getDescricao(), reposta.getMarca(), reposta.getPreco(), 10, 0,
                reposta.getCodigoReferencia(), reposta.getCategoria())));
        fluxo.clienteAprova(orcamento);
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_PAGAMENTO);
        assertThat(peca(pecaId).getQuantidadeReservada()).isEqualTo(2);
    }

    @Test
    void pagamentoRecusadoLiberaAsPecasCancelaOOrcamentoEAOrdem() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = diagnosticadaComPeca("SAG5A55", pecaId, 2);
        String orcamento = ateAguardarPagamento(numero);

        fluxo.simularPagamento(orcamento, "RECUSADO").andExpect(status().isOk());
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.CANCELADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.PAGAMENTO_RECUSADO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSADA);
        assertThat(fluxo.orcamento(numero).orElseThrow().getStatus()).isEqualTo(StatusOrcamento.CANCELADO);
        assertThat(pagamentoDaOrdem(numero).getStatus()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(peca(pecaId).getQuantidadeReservada()).isZero();
        assertThat(peca(pecaId).getQuantidadeEstoque()).isEqualTo(5);

        fluxo.simularPagamento(orcamento, "APROVADO").andExpect(status().isConflict());
    }

    @Test
    void pagamentoExpiradoNoBillingCancelaAOrdem() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = diagnosticadaComPeca("SAG6A66", pecaId, 1);
        ateAguardarPagamento(numero);

        assertThat(resultadoPagamento.expirarVencidos(LocalDateTime.now())).as("ainda no prazo").isZero();
        assertThat(resultadoPagamento.expirarVencidos(LocalDateTime.now().plusDays(2))).isEqualTo(1);
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.CANCELADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.PAGAMENTO_EXPIRADO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSADA);
        assertThat(pagamentoDaOrdem(numero).getStatus()).isEqualTo(StatusPagamento.EXPIRADO);
        assertThat(peca(pecaId).getQuantidadeReservada()).isZero();
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
        assertThat(peca(pecaId).getQuantidadeReservada()).isZero();
    }

    @Test
    void execucaoAbortadaEstornaOPagamentoELiberaAsPecas() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = diagnosticadaComPeca("SAG8A88", pecaId, 2);
        String orcamento = ateAguardarPagamento(numero);
        fluxo.simularPagamento(orcamento, "APROVADO").andExpect(status().isOk());
        fluxo.processarMensagens();
        fluxo.acao(numero, "/execucao/iniciar");

        fluxo.abortarExecucao(numero, "Peca errada");
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.CANCELADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.FALHA_EXECUCAO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSADA);
        assertThat(pagamentoDaOrdem(numero).getStatus()).isEqualTo(StatusPagamento.ESTORNADO);
        assertThat(pagamentoDaOrdem(numero).getMpRefundId()).startsWith("SIM-REFUND-");
        assertThat(fluxo.orcamento(numero).orElseThrow().getStatus()).isEqualTo(StatusOrcamento.CANCELADO);
        assertThat(peca(pecaId).getQuantidadeReservada()).isZero();
        assertThat(peca(pecaId).getQuantidadeEstoque()).isEqualTo(5);
        assertThat(detalhesDosPassos(numero)).anySatisfy(d -> assertThat(d).contains("Peca errada"));
    }

    @Test
    void orcamentoComPecaQueSumiuDoCatalogoReabreODiagnostico() throws Exception {
        String pecaId = fluxo.cadastrarPeca("Filtro", 5);
        String numero = diagnosticadaComPeca("SAG9A99", pecaId, 1);
        springDataPecas.deleteById(pecaId);

        fluxo.solicitarOrcamento(numero);
        fluxo.processarMensagens();

        assertThat(fluxo.orcamento(numero)).isEmpty();
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga(numero).getMotivo()).contains(pecaId);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO);
        assertThat(fluxo.ordem(numero).getPecasPrevistas()).as("pecas saem").isEmpty();
        assertThat(fluxo.ordem(numero).getServicos()).as("servicos ficam").isNotEmpty();
        assertThat(detalhesDosPassos(numero)).anySatisfy(d -> assertThat(d).contains("diagnostico reaberto"));
    }

    @Test
    void orcamentoRecusadoPeloBillingSemPecaInvalidaMantemODiagnosticoEPodeSerReenviado() throws Exception {
        String numero = diagnosticada("SAG1C11");

        fluxo.solicitarOrcamento(numero, new BigDecimal("200.00"), new BigDecimal("1000.00"));
        fluxo.processarMensagens();

        assertThat(fluxo.orcamento(numero)).isEmpty();
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga(numero).getMotivo()).contains("Desconto");
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO);

        fluxo.enviarParaOrcamento(numero);

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.AGUARDANDO_APROVACAO);
    }

    @Test
    void comandoSemRespostaEhReenviadoEDepoisTratadoComoFalha() throws Exception {
        String numero = diagnosticada("SAG1D11");
        fluxo.solicitarOrcamento(numero);
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

        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga(numero).getMotivo()).contains("nao respondeu");
        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO);

        // Os reenvios ainda pendentes chegam atrasados: o Billing gera um orcamento so e responde a todos,
        // mas a Saga ja desistiu e ignora. O novo envio encontra o orcamento e segue.
        fluxo.processarMensagens();
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(tiposDosPassos(numero)).contains(RegistroPassoSaga.Tipo.EVENTO_IGNORADO);

        fluxo.enviarParaOrcamento(numero);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
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
        assertThat(peca(pecaId).getQuantidadeReservada()).isZero();
    }

    @Test
    void rejeicaoEncerraAOrdemEASagaSemExecucao() throws Exception {
        String numero = diagnosticada("SAG1F11");
        String orcamento = fluxo.enviarParaOrcamento(numero);

        fluxo.clienteRejeita(orcamento);
        fluxo.processarMensagens();

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.OS_FINALIZADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.ORCAMENTO_RECUSADO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
    }

    @Test
    void prazoDeDecisaoVencidoCancelaOOrcamentoPorCompensacaoEEncerraAOrdem() throws Exception {
        String numero = diagnosticada("SAG1G11");
        fluxo.enviarParaOrcamento(numero);

        assertThat(orquestrador.processarPrazosVencidos(VALIDADE.minusDays(1))).as("ainda no prazo").isZero();
        assertThat(orquestrador.processarPrazosVencidos(VALIDADE.plusDays(1))).isEqualTo(1);

        assertThat(fluxo.ordem(numero).getStatus()).isEqualTo(StatusOrdemDeServico.OS_FINALIZADA);
        assertThat(fluxo.ordem(numero).getMotivoEncerramento()).isEqualTo(MotivoEncerramento.ORCAMENTO_EXPIRADO);
        assertThat(ultimaAlteracao(numero).origem()).isEqualTo(OrigemAlteracaoStatus.TIMEOUT);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        assertThat(outbox.findAll()).extracting(m -> m.getTipo()).contains(MensagensSaga.CANCELAR_ORCAMENTO);

        fluxo.processarMensagens();

        assertThat(fluxo.orcamento(numero).orElseThrow().getStatus()).isEqualTo(StatusOrcamento.CANCELADO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(saga(numero).getCompensacoesPendentes()).isEmpty();
        assertThat(tiposDosPassos(numero)).contains(
                RegistroPassoSaga.Tipo.TIMEOUT, RegistroPassoSaga.Tipo.COMANDO_ENVIADO, RegistroPassoSaga.Tipo.EVENTO_RECEBIDO);
    }

    @Test
    void confirmacaoPerdidaEhPedidaDeNovoEOParticipanteRepublicaPeloEstado() throws Exception {
        String numero = diagnosticada("SAG1H11");
        fluxo.enviarParaOrcamento(numero);
        UUID orcamentoId = fluxo.orcamento(numero).orElseThrow().getId();
        orquestrador.processarPrazosVencidos(VALIDADE.plusDays(1));
        fluxo.processarMensagens();
        // Simula a resposta perdida: a Saga volta a esperar a confirmacao, mas o orcamento ja esta cancelado.
        transacao.executeWithoutResult(s -> {
            SagaOrdemServico emCompensacao = SagaOrdemServico.iniciar(fluxo.ordem(numero).getId(), LocalDateTime.now());
            springDataSagas.deleteAllInBatch();
            emCompensacao.solicitarOrcamento(null, LocalDateTime.now(), Duration.ofMinutes(5));
            emCompensacao.orcamentoGerado(orcamentoId, VALIDADE, LocalDateTime.now());
            emCompensacao.prazoDecisaoVencido(VALIDADE.plusDays(1), Duration.ofMinutes(5));
            emCompensacao.descarregarComandos();
            springDataSagas.save(emCompensacao);
        });

        assertThat(orquestrador.processarPrazosVencidos(VALIDADE.plusDays(1).plusMinutes(10))).isEqualTo(1);
        assertThat(saga(numero).getTentativasPasso()).isEqualTo(1);
        fluxo.processarMensagens();

        assertThat(saga(numero).getStatus()).as("o reenvio encontrou o orcamento ja cancelado e confirmou de novo")
                .isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
    }

    @Test
    void semConfirmacaoAposTodasAsTentativasASagaFicaEmFalhaDeCompensacao() throws Exception {
        String numero = diagnosticada("SAG1I11");
        fluxo.enviarParaOrcamento(numero);
        LocalDateTime agora = VALIDADE.plusDays(1);
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
    void acoesForaDoEstadoEsperadoSaoRegistradasComoIgnoradas() throws Exception {
        String numero = fluxo.abrirOrdem("SAG1L11");
        UUID ordemId = fluxo.ordem(numero).getId();

        orquestrador.execucaoIniciada(ordemId);
        orquestrador.execucaoConcluida(ordemId);
        orquestrador.execucaoFalhou(ordemId, "sem execucao");

        assertThat(tiposDosPassos(numero)).containsExactly(
                RegistroPassoSaga.Tipo.INICIO,
                RegistroPassoSaga.Tipo.EVENTO_IGNORADO,
                RegistroPassoSaga.Tipo.EVENTO_IGNORADO,
                RegistroPassoSaga.Tipo.EVENTO_IGNORADO);
        assertThat(saga(numero).getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
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
