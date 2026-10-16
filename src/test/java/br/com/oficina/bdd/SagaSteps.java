package br.com.oficina.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.saga.application.OrquestradorSagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.model.RegistroPassoSaga;
import br.com.oficina.ordemservico.saga.domain.model.SagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.repository.SagaOrdemServicoRepository;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;
import br.com.oficina.pecainsumo.infrastructure.persistence.SpringDataPecaInsumoRepository;
import br.com.oficina.support.saga.FluxoOficinaHttp;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;

/**
 * Passos dos cenarios da Saga. O lado do OS passa pelas APIs HTTP reais; o do Billing (orcamento,
 * decisao do cliente, pagamento) e o {@code BillingSimulado}, que responde como o contrato manda. A
 * mensageria assincrona e "esperada" processando o outbox antes de cada verificacao - o que o
 * agendamento faria em ~1 s.
 */
public class SagaSteps {

    /** Validade do orcamento enviado nos cenarios (FluxoOficinaHttp#solicitarOrcamento). */
    private static final LocalDateTime DEPOIS_DA_VALIDADE = LocalDateTime.of(2030, 1, 2, 0, 0);

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private OrquestradorSagaOrdemServico orquestrador;

    @Autowired
    private SagaOrdemServicoRepository sagas;

    @Autowired
    private PecaInsumoRepository pecaInsumoRepository;

    @Autowired
    private SpringDataPecaInsumoRepository springDataPecas;

    private FluxoOficinaHttp fluxo;
    private String numeroOrdem;
    private String numeroOrcamento;
    private final Map<String, String> pecas = new HashMap<>();

    @Before
    public void preparar() {
        fluxo = new FluxoOficinaHttp(MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build(), contexto);
        fluxo.limpar();
        pecas.clear();
    }

    @After
    public void limpar() {
        fluxo.limpar();
    }

    // ------------------------------------------------------------------ preparacao

    @Dado("a peça {string} com {int} unidades em estoque")
    public void pecaEmEstoque(String descricao, int estoque) {
        pecas.put(descricao, fluxo.cadastrarPeca(descricao, estoque));
    }

    @Dado("que restou só {int} unidade de {string}")
    public void restouSo(int estoque, String descricao) {
        PecaInsumo peca = fluxo.peca(pecas.get(descricao));
        pecaInsumoRepository.salvar(new PecaInsumo(peca.getId(), peca.getDescricao(), peca.getMarca(), peca.getPreco(),
                estoque, 0, peca.getCodigoReferencia(), peca.getCategoria()));
    }

    @Dado("que a peça {string} saiu do catálogo")
    public void pecaSaiuDoCatalogo(String descricao) {
        springDataPecas.deleteById(pecas.get(descricao));
    }

    @Dado("uma ordem de serviço aberta para o veículo {string}")
    public void umaOrdemAberta(String placa) throws Exception {
        numeroOrdem = fluxo.abrirOrdem(placa);
    }

    @E("o diagnóstico concluído com o serviço {string}")
    public void diagnosticoConcluido(String servico) throws Exception {
        fluxo.diagnosticar(numeroOrdem, servico);
    }

    @E("o diagnóstico concluído com o serviço {string} e {int} unidades de {string}")
    public void diagnosticoConcluidoComPeca(String servico, int quantidade, String descricao) throws Exception {
        fluxo.diagnosticarComPeca(numeroOrdem, servico, pecas.get(descricao), quantidade);
    }

    @E("o orçamento enviado ao cliente")
    public void orcamentoEnviado() throws Exception {
        numeroOrcamento = fluxo.enviarParaOrcamento(numeroOrdem);
    }

    // ------------------------------------------------------------------ acoes

    @Quando("o diagnóstico é enviado para orçamento")
    public void diagnosticoEnviado() throws Exception {
        fluxo.solicitarOrcamento(numeroOrdem);
    }

    @Quando("o cliente aprova o orçamento")
    public void clienteAprova() throws Exception {
        fluxo.clienteAprova(numeroOrcamento);
    }

    @Quando("o cliente rejeita o orçamento")
    public void clienteRejeita() throws Exception {
        fluxo.clienteRejeita(numeroOrcamento);
    }

    @Quando("o cliente paga")
    public void clientePaga() {
        fluxo.processarMensagens();
        fluxo.simularPagamento(numeroOrcamento, "APROVADO");
    }

    @Quando("o pagamento é recusado")
    public void pagamentoRecusado() {
        fluxo.processarMensagens();
        fluxo.simularPagamento(numeroOrcamento, "RECUSADO");
    }

    @Quando("o prazo de pagamento vence")
    public void prazoDePagamentoVence() {
        fluxo.processarMensagens();
        fluxo.expirarPagamento(numeroOrcamento);
    }

    @Quando("o mecânico inicia e conclui a execução")
    public void mecanicoExecuta() throws Exception {
        fluxo.processarMensagens();
        fluxo.acao(numeroOrdem, "/execucao/iniciar");
        fluxo.acao(numeroOrdem, "/servico/concluir");
    }

    @Quando("o mecânico inicia a execução e aborta com o motivo {string}")
    public void mecanicoAborta(String motivo) throws Exception {
        fluxo.processarMensagens();
        fluxo.acao(numeroOrdem, "/execucao/iniciar");
        fluxo.abortarExecucao(numeroOrdem, motivo);
    }

    @Quando("o prazo de decisão vence sem resposta do cliente")
    public void prazoVence() {
        orquestrador.processarPrazosVencidos(DEPOIS_DA_VALIDADE);
    }

    // ------------------------------------------------------------------ verificacoes

    @Entao("a ordem fica {string}")
    public void ordemFica(String situacao) throws Exception {
        fluxo.processarMensagens();
        assertThat(fluxo.ordem(numeroOrdem).getSituacao().getDescricao()).isEqualTo(situacao);
    }

    @Entao("a ordem fica {string} com motivo {string}")
    public void ordemFicaComMotivo(String situacao, String motivo) throws Exception {
        ordemFica(situacao);
        assertThat(fluxo.ordem(numeroOrdem).getMotivoEncerramento()).hasToString(motivo);
    }

    @E("o diagnóstico volta ao mecânico sem as peças")
    public void diagnosticoReaberto() {
        OrdemDeServico ordem = fluxo.ordem(numeroOrdem);
        assertThat(ordem.getStatus()).isEqualTo(StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO);
        assertThat(ordem.getPecasPrevistas()).isEmpty();
        assertThat(ordem.getServicos()).isNotEmpty();
    }

    @E("a saga fica {string}")
    public void sagaFica(String status) throws Exception {
        fluxo.processarMensagens();
        fluxo.saga(numeroOrdem)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status));
    }

    @E("a saga termina como {string}")
    public void sagaTermina(String status) throws Exception {
        sagaFica(status);
        assertThat(saga().getStatus().isFinal()).as("estado final").isTrue();
    }

    @E("o cliente recebe o link de pagamento")
    public void linkDePagamento() throws Exception {
        fluxo.processarMensagens();
        String link = saga().getLinkPagamento();
        assertThat(link).isNotBlank();
        assertThat(fluxo.pagamento(numeroOrcamento)).hasValueSatisfying(pagamento -> {
            assertThat(pagamento.getStatus()).hasToString("PENDENTE");
            assertThat(pagamento.getLinkPagamento()).isEqualTo(link);
        });
    }

    @E("o orçamento fica {string}")
    public void orcamentoFica(String status) {
        fluxo.processarMensagens();
        assertThat(fluxo.orcamento(numeroOrdem))
                .hasValueSatisfying(orcamento -> assertThat(orcamento.getStatus()).hasToString(status));
    }

    @E("nenhum orçamento é gerado")
    public void nenhumOrcamento() {
        fluxo.processarMensagens();
        assertThat(fluxo.orcamento(numeroOrdem)).isEmpty();
    }

    @E("o pagamento fica {string}")
    public void pagamentoFica(String status) {
        fluxo.processarMensagens();
        assertThat(fluxo.pagamento(numeroOrcamento))
                .hasValueSatisfying(pagamento -> assertThat(pagamento.getStatus()).hasToString(status));
    }

    @E("a peça {string} fica com {int} unidades reservadas e {int} em estoque")
    public void pecaFica(String descricao, int reservadas, int estoque) {
        fluxo.processarMensagens();
        PecaInsumo peca = fluxo.peca(pecas.get(descricao));
        assertThat(peca.getQuantidadeReservada()).as("reservadas").isEqualTo(reservadas);
        assertThat(peca.getQuantidadeEstoque()).as("em estoque").isEqualTo(estoque);
    }

    @E("a história da saga registra o comando {string} e a confirmação {string}")
    public void historiaRegistra(String comando, String confirmacao) {
        fluxo.processarMensagens();
        List<RegistroPassoSaga> passos = sagas.listarPassos(saga().getId());
        assertThat(passos).anySatisfy(p -> {
            assertThat(p.getTipo()).isEqualTo(RegistroPassoSaga.Tipo.COMANDO_ENVIADO);
            assertThat(p.getDetalhe()).isEqualTo(comando);
        });
        assertThat(passos).anySatisfy(p -> {
            assertThat(p.getTipo()).isEqualTo(RegistroPassoSaga.Tipo.EVENTO_RECEBIDO);
            assertThat(p.getDetalhe()).startsWith(confirmacao);
        });
    }

    private SagaOrdemServico saga() {
        return sagas.buscarPorOrdemDeServico(fluxo.ordem(numeroOrdem).getId()).orElseThrow();
    }
}
