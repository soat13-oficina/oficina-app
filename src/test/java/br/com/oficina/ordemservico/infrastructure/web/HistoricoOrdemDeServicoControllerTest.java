package br.com.oficina.ordemservico.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.cliente.infrastructure.persistence.SpringDataClienteRepository;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataFuncionarioRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataHistoricoStatusOrdemDeServicoRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataOrdemDeServicoRepository;
import br.com.oficina.support.saga.FluxoOficinaHttp;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;

/** {@code GET /ordens-servico/{numero}/historico}: as mudancas de status gravadas junto com a ordem. */
@SpringBootTest
class HistoricoOrdemDeServicoControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private OrdemDeServicoRepository ordemDeServicoRepository;

    @Autowired
    private SpringDataHistoricoStatusOrdemDeServicoRepository springDataHistoricoRepository;

    @Autowired
    private SpringDataOrdemDeServicoRepository springDataOrdemDeServicoRepository;

    @Autowired
    private SpringDataVeiculoRepository springDataVeiculoRepository;

    @Autowired
    private SpringDataClienteRepository springDataClienteRepository;

    @Autowired
    private SpringDataFuncionarioRepository springDataFuncionarioRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        // Zera tambem as Sagas, o outbox e os servicos simulados que outros testes deixaram.
        new FluxoOficinaHttp(mockMvc, context).limpar();
        springDataHistoricoRepository.deleteAll();
        springDataOrdemDeServicoRepository.deleteAll();
        springDataVeiculoRepository.deleteAll();
        springDataClienteRepository.deleteAll();
        springDataFuncionarioRepository.deleteAll();
    }

    @Test
    void historicoTrazAAberturaEOInicioDoDiagnosticoFeitoPeloMecanico() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Maria", "20110101103", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", "12345678909"));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "HIS1D23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        mockMvc.perform(post("/ordens-servico").with(user("tester")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "clienteId": "%s", "funcionarioId": "%s", "placaVeiculo": "HIS1D23" }
                                """.formatted(cliente.getId(), funcionario.getId())))
                .andExpect(status().isCreated());
        String numero = ordemDeServicoRepository.buscarTodas().get(0).getNumeroOrdemServico();

        // O mecanico pega a OS na fila da Execution; a Saga move a ordem para "Diagnostico" ao receber o evento.
        FluxoOficinaHttp fluxo = new FluxoOficinaHttp(mockMvc, context);
        fluxo.processarMensagens();
        fluxo.iniciarDiagnostico(numero);

        mockMvc.perform(get("/ordens-servico/" + numero + "/historico").with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroOrdemServico").value(numero))
                .andExpect(jsonPath("$.situacaoAtual").value("Diagnóstico"))
                .andExpect(jsonPath("$.alteracoes.length()").value(2))
                .andExpect(jsonPath("$.alteracoes[0].statusAnterior").doesNotExist())
                .andExpect(jsonPath("$.alteracoes[0].statusNovo").value("OS_ABERTA"))
                .andExpect(jsonPath("$.alteracoes[0].situacao").value("Recebida"))
                .andExpect(jsonPath("$.alteracoes[0].origem").value("API"))
                .andExpect(jsonPath("$.alteracoes[1].statusAnterior").value("OS_ABERTA"))
                .andExpect(jsonPath("$.alteracoes[1].statusNovo").value("DIAGNOSTICO_EM_ANDAMENTO"))
                .andExpect(jsonPath("$.alteracoes[1].situacao").value("Diagnóstico"))
                .andExpect(jsonPath("$.alteracoes[1].origem").value("SAGA"))
                .andExpect(jsonPath("$.alteracoes[1].sagaId").exists());
    }

    @Test
    void ordemInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/ordens-servico/OS-NAO-EXISTE/historico").with(user("tester")))
                .andExpect(status().isNotFound());
    }

    @Test
    void historicoExigeAutenticacao() throws Exception {
        mockMvc.perform(get("/ordens-servico/OS-QUALQUER/historico"))
                .andExpect(status().isForbidden());
    }
}
