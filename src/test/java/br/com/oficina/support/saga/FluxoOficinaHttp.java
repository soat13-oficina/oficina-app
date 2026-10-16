package br.com.oficina.support.saga;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;


import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.cliente.infrastructure.persistence.SpringDataClienteRepository;
import br.com.oficina.common.infrastructure.mensageria.PublicadorOutbox;
import br.com.oficina.common.infrastructure.mensageria.SpringDataMensagemProcessadaRepository;
import br.com.oficina.common.infrastructure.mensageria.SpringDataOutboxMensagemRepository;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataFuncionarioRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataHistoricoStatusOrdemDeServicoRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataOrdemDeServicoRepository;
import br.com.oficina.ordemservico.saga.infrastructure.persistence.SpringDataRegistroPassoSagaRepository;
import br.com.oficina.ordemservico.saga.infrastructure.persistence.SpringDataSagaOrdemServicoRepository;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;
import br.com.oficina.pecainsumo.infrastructure.persistence.SpringDataPecaInsumoRepository;
import br.com.oficina.pecainsumo.infrastructure.persistence.SpringDataReservaPecaRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;

/**
 * Conduz o ciclo de vida da OS pelas APIs HTTP reais (MockMvc), como o atendente e o mecanico
 * fariam - usado pelo teste do orquestrador, pelos passos do BDD e pelo teste de contrato. O lado do
 * Billing (orcamento, decisao do cliente, pagamento) e o {@link BillingSimulado}.
 *
 * <p>A mensageria roda em memoria e o agendamento do outbox esta desligado nos testes: o que um
 * pod faria a cada segundo, aqui e {@link #processarMensagens()}.
 */
public class FluxoOficinaHttp {

    public static final RequestPostProcessor ATENDENTE = user("atendente@oficina.com");
    public static final RequestPostProcessor CLIENTE = user("52998224725").roles("CLIENTE");

    private final MockMvc mvc;
    private final WebApplicationContext contexto;

    public FluxoOficinaHttp(MockMvc mvc, WebApplicationContext contexto) {
        this.mvc = mvc;
        this.contexto = contexto;
    }

    private <T> T bean(Class<T> tipo) {
        return contexto.getBean(tipo);
    }

    /** Apaga, na ordem das dependencias, tudo o que os cenarios criam. */
    public void limpar() {
        bean(SpringDataRegistroPassoSagaRepository.class).deleteAll();
        bean(SpringDataSagaOrdemServicoRepository.class).deleteAll();
        bean(SpringDataHistoricoStatusOrdemDeServicoRepository.class).deleteAll();
        bean(SpringDataOutboxMensagemRepository.class).deleteAll();
        bean(SpringDataMensagemProcessadaRepository.class).deleteAll();
        billing().limpar();
        bean(SpringDataReservaPecaRepository.class).deleteAll();
        bean(SpringDataOrdemDeServicoRepository.class).deleteAll();
        bean(SpringDataVeiculoRepository.class).deleteAll();
        bean(SpringDataClienteRepository.class).deleteAll();
        bean(SpringDataFuncionarioRepository.class).deleteAll();
        bean(SpringDataPecaInsumoRepository.class).deleteAll();
    }

    /** Peca no cadastro com {@code estoque} unidades disponiveis; devolve o id. */
    public String cadastrarPeca(String descricao, int estoque) {
        PecaInsumo peca = new PecaInsumo(descricao, "Bosch", new BigDecimal("80.00"), estoque, "REF-" + descricao, CategoriaPeca.FILTROS);
        bean(PecaInsumoRepository.class).salvar(peca);
        return peca.getId();
    }

    public PecaInsumo peca(String pecaInsumoId) {
        return bean(PecaInsumoRepository.class).buscarPorId(pecaInsumoId).orElseThrow();
    }

    /** Cliente, funcionario e veiculo; devolve o numero da OS aberta por POST /ordens-servico. */
    public String abrirOrdem(String placa) throws Exception {
        // Mesmo cliente e funcionario para todas as OSs do cenario (documento e CPF sao unicos).
        Cliente cliente = bean(ClienteRepository.class).buscarPorDocumento("52998224725")
                .orElseGet(() -> bean(ClienteRepository.class).salvar(new Cliente("Maria", "52998224725", TipoCliente.PF)));
        Funcionario funcionario = bean(SpringDataFuncionarioRepository.class).findByCpf("12345678909")
                .orElseGet(() -> bean(SpringDataFuncionarioRepository.class).save(new Funcionario("Ana Atendente", "12345678909")));
        bean(VeiculoRepository.class).salvar(new Veiculo(
                cliente.getId(), placa, "Toyota", "Corolla", "Toyota Motor Corporation", 2020, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        mvc.perform(post("/ordens-servico").with(ATENDENTE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "clienteId": "%s", "funcionarioId": "%s", "placaVeiculo": "%s" }
                                """.formatted(cliente.getId(), funcionario.getId(), placa)))
                .andExpect(status().isCreated());
        return bean(OrdemDeServicoRepository.class).buscarTodas().stream()
                .filter(os -> placa.equals(os.getVeiculo().getPlaca()))
                .findFirst().orElseThrow().getNumeroOrdemServico();
    }

    public void diagnosticar(String numero, String descricao) throws Exception {
        diagnosticarComPeca(numero, descricao, null, 0);
    }

    /** Diagnostico com uma peca prevista ({@code pecaInsumoId} nulo = sem pecas). */
    public void diagnosticarComPeca(String numero, String descricao, String pecaInsumoId, int quantidade) throws Exception {
        acao(numero, "/diagnostico/iniciar");
        String pecas = pecaInsumoId == null ? "[]"
                : "[ { \"pecaInsumoId\": \"%s\", \"quantidade\": %d } ]".formatted(pecaInsumoId, quantidade);
        mvc.perform(post("/ordens-servico/" + numero + "/diagnostico/concluir").with(ATENDENTE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"descricaoServico\": \"%s\", \"pecas\": %s }".formatted(descricao, pecas)))
                .andExpect(status().isNoContent());
    }

    /** Envia o diagnostico para orcamento (202): so grava o {@code GerarOrcamento} no outbox. */
    public void solicitarOrcamento(String numero) throws Exception {
        solicitarOrcamento(numero, new BigDecimal("200.00"), BigDecimal.ZERO);
    }

    public void solicitarOrcamento(String numero, BigDecimal valorMaoDeObra, BigDecimal desconto) throws Exception {
        mvc.perform(post("/ordens-servico/" + numero + "/diagnostico/enviar-para-orcamento").with(ATENDENTE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "valorMaoDeObra": %s, "desconto": %s, "validade": "2030-01-01T00:00:00", "observacoes": null }
                                """.formatted(valorMaoDeObra, desconto)))
                .andExpect(status().isAccepted());
    }

    /** Envia para orcamento, entrega as mensagens e devolve o numero do orcamento gerado pelo Billing. */
    public String enviarParaOrcamento(String numero) throws Exception {
        solicitarOrcamento(numero);
        processarMensagens();
        return orcamento(numero).orElseThrow(() -> new IllegalStateException("Orcamento nao gerado para " + numero))
                .getNumeroOrcamento();
    }

    public BillingSimulado billing() {
        return bean(BillingSimulado.class);
    }

    /** Orcamento que o Billing (simulado) gerou para a OS. */
    public Optional<BillingSimulado.OrcamentoSimulado> orcamento(String numeroOrdem) {
        return billing().orcamentoDaOrdem(ordem(numeroOrdem).getId());
    }

    public void clienteAprova(String numeroOrcamento) {
        billing().decidir(numeroOrcamento, true, "CLIENTE");
    }

    public void clienteRejeita(String numeroOrcamento) {
        billing().decidir(numeroOrcamento, false, "CLIENTE");
    }

    public Optional<BillingSimulado.PagamentoSimulado> pagamento(String numeroOrcamento) {
        return billing().pagamentoDoOrcamento(numeroOrcamento);
    }

    public UUID pagamentoId(String numeroOrcamento) {
        return pagamento(numeroOrcamento).orElseThrow(() -> new IllegalStateException("Orcamento sem cobranca: " + numeroOrcamento))
                .getId();
    }

    /** O cliente paga ({@code APROVADO}) ou o cartao e recusado ({@code RECUSADO}): checkout + webhook do provedor. */
    public void simularPagamento(String numeroOrcamento, String resultado) {
        if ("APROVADO".equals(resultado)) {
            billing().pagar(numeroOrcamento);
        } else {
            billing().recusarPagamento(numeroOrcamento);
        }
    }

    /** O Billing expira a cobranca vencida. */
    public void expirarPagamento(String numeroOrcamento) {
        billing().expirarPagamento(numeroOrcamento);
    }

    public void acao(String numero, String caminho) throws Exception {
        mvc.perform(post("/ordens-servico/" + numero + caminho).with(ATENDENTE).with(csrf()))
                .andExpect(status().isNoContent());
    }

    public void abortarExecucao(String numero, String motivo) throws Exception {
        mvc.perform(post("/ordens-servico/" + numero + "/execucao/abortar").with(ATENDENTE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"motivo\": \"%s\" }".formatted(motivo)))
                .andExpect(status().isNoContent());
    }

    /** Publica o outbox ate esvaziar - inclusive as respostas geradas pelo proprio processamento. */
    public int processarMensagens() {
        PublicadorOutbox publicador = bean(PublicadorOutbox.class);
        int total = 0;
        for (int ciclo = 0; ciclo < 20; ciclo++) {
            int entregues = publicador.publicarPendentes();
            if (entregues == 0) {
                return total;
            }
            total += entregues;
        }
        throw new IllegalStateException("Outbox nao esvaziou em 20 ciclos");
    }

    /** Um ciclo do publicador: entrega so o que ja estava pendente; as respostas ficam para o proximo. */
    public int processarUmCiclo() {
        return bean(PublicadorOutbox.class).publicarPendentes();
    }

    /**
     * Simula a perda no transporte: as mensagens pendentes do tipo saem do outbox sem chegar ao
     * destino (o participante nunca responde). O reenvio da Saga continua achando o original.
     */
    public int perderMensagensPendentes(String tipo) {
        return bean(JdbcTemplate.class).update(
                "UPDATE outbox_mensagens SET publicada_em = CURRENT_TIMESTAMP WHERE tipo = ? AND publicada_em IS NULL", tipo);
    }

    public OrdemDeServico ordem(String numero) {
        return bean(OrdemDeServicoRepository.class).buscarPorNumero(numero).orElseThrow();
    }

    public ResultActions saga(String numero) throws Exception {
        return mvc.perform(get("/sagas/" + ordem(numero).getId()).with(ATENDENTE));
    }
}
