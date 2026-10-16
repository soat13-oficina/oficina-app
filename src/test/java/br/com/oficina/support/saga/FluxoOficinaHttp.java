package br.com.oficina.support.saga;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.http.MediaType;
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
import br.com.oficina.orcamento.infrastructure.persistence.SpringDataOrcamentoRepository;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataFuncionarioRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataHistoricoStatusOrdemDeServicoRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataOrdemDeServicoRepository;
import br.com.oficina.ordemservico.saga.infrastructure.persistence.SpringDataRegistroPassoSagaRepository;
import br.com.oficina.ordemservico.saga.infrastructure.persistence.SpringDataSagaOrdemServicoRepository;
import br.com.oficina.pecainsumo.infrastructure.persistence.SpringDataPecaInsumoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;

/**
 * Conduz o ciclo de vida da OS pelas APIs HTTP reais (MockMvc), como o atendente, o mecanico e o
 * cliente fariam - usado pelo teste do orquestrador e pelos passos do BDD.
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
        bean(SpringDataOrcamentoRepository.class).deleteAll();
        bean(SpringDataOrdemDeServicoRepository.class).deleteAll();
        bean(SpringDataVeiculoRepository.class).deleteAll();
        bean(SpringDataClienteRepository.class).deleteAll();
        bean(SpringDataFuncionarioRepository.class).deleteAll();
        bean(SpringDataPecaInsumoRepository.class).deleteAll();
    }

    /** Cliente, funcionario e veiculo; devolve o numero da OS aberta por POST /ordens-servico. */
    public String abrirOrdem(String placa) throws Exception {
        Cliente cliente = bean(ClienteRepository.class).salvar(new Cliente("Maria", "52998224725", TipoCliente.PF));
        Funcionario funcionario = bean(SpringDataFuncionarioRepository.class).save(new Funcionario("Ana Atendente", "12345678909"));
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
        acao(numero, "/diagnostico/iniciar");
        mvc.perform(post("/ordens-servico/" + numero + "/diagnostico/concluir").with(ATENDENTE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"descricaoServico\": \"%s\", \"pecas\": [] }".formatted(descricao)))
                .andExpect(status().isNoContent());
    }

    /** Envia o diagnostico para orcamento e devolve o numero do orcamento criado. */
    public String enviarParaOrcamento(String numero) throws Exception {
        mvc.perform(post("/ordens-servico/" + numero + "/diagnostico/enviar-para-orcamento").with(ATENDENTE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "valorMaoDeObra": 200.00, "desconto": 0, "validade": "2030-01-01T00:00:00", "observacoes": null }
                                """))
                .andExpect(status().isNoContent());
        UUID ordemId = ordem(numero).getId();
        return bean(SpringDataOrcamentoRepository.class).findAll().stream()
                .filter(o -> ordemId.equals(o.getOrdemDeServicoId()))
                .findFirst().orElseThrow().getNumeroOrcamento();
    }

    public void clienteAprova(String numeroOrcamento) throws Exception {
        mvc.perform(post("/orcamentos/" + numeroOrcamento + "/aprovacao").with(CLIENTE).with(csrf()))
                .andExpect(status().isOk());
    }

    public void clienteRejeita(String numeroOrcamento) throws Exception {
        mvc.perform(post("/orcamentos/" + numeroOrcamento + "/rejeicao").with(CLIENTE).with(csrf()))
                .andExpect(status().isNoContent());
    }

    public void acao(String numero, String caminho) throws Exception {
        mvc.perform(post("/ordens-servico/" + numero + caminho).with(ATENDENTE).with(csrf()))
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

    public OrdemDeServico ordem(String numero) {
        return bean(OrdemDeServicoRepository.class).buscarPorNumero(numero).orElseThrow();
    }

    public ResultActions saga(String numero) throws Exception {
        return mvc.perform(get("/sagas/" + ordem(numero).getId()).with(ATENDENTE));
    }
}
