package br.com.oficina.ordemservico.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@SpringBootTest
class OrdemDeServicoControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private OrdemDeServicoRepository ordemDeServicoRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveCriarOrdemDeServico() throws Exception {
        clienteRepository.salvar(new Cliente("cliente-os-1", "Maria", "11111111111"));
        veiculoRepository.salvar(new Veiculo(
                "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));

        String requestBody = """
                {
                  "clienteId": "cliente-os-1",
                  "funcionarioId": "func-1",
                  "placaVeiculo": "ABC1D23"
                }
                """;

        mockMvc.perform(post("/ordens-servico")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted());
    }

    @Test
    void deveAlterarOrdemDeServico() throws Exception {
        clienteRepository.salvar(new Cliente("cliente-os-alt-1", "Marina", "12345678901"));
        clienteRepository.salvar(new Cliente("cliente-os-alt-2", "Roberta", "99999999999"));
        veiculoRepository.salvar(new Veiculo(
                "ALT1A11", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        veiculoRepository.salvar(new Veiculo(
                "ALT2B22", "Honda", "City", "Honda Motor Co.", 2023, 126, "AUTOMATICO", TipoCombustivel.FLEX));

        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                "id-os-alterar-1",
                "OS-ALTERAR-1",
                new Funcionario("func-20", "Joao", null),
                new Cliente("cliente-os-alt-1", "Marina", "12345678901"),
                new Veiculo("ALT1A11", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        ordemDeServicoRepository.salvar(ordemDeServico);

        String requestBody = """
                {
                  "clienteId": "cliente-os-alt-2",
                  "funcionarioId": "func-21",
                  "placaVeiculo": "ALT2B22"
                }
                """;

        mockMvc.perform(put("/ordens-servico/OS-ALTERAR-1")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        OrdemDeServico ordemAtualizada = ordemDeServicoRepository.buscarPorNumero("OS-ALTERAR-1")
                .orElseThrow();
        assert ordemAtualizada.getCliente().getId().equals("cliente-os-alt-2");
        assert ordemAtualizada.getVeiculo().getPlaca().equals("ALT2B22");
        assert ordemAtualizada.getFuncionario().getId().equals("func-21");
    }

    @Test
    void deveExcluirOrdemDeServico() throws Exception {
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                "id-os-excluir-1",
                "OS-EXCLUIR-1",
                new Funcionario("func-30", "Joao", null),
                new Cliente("cliente-os-excluir-1", "Marina", "12345678901"),
                new Veiculo("DEL1O23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        ordemDeServicoRepository.salvar(ordemDeServico);

        mockMvc.perform(delete("/ordens-servico/OS-EXCLUIR-1")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assert ordemDeServicoRepository.buscarPorNumero("OS-EXCLUIR-1").isEmpty();
    }

    @Test
    void deveConsultarOrdemDeServicoPorFiltros() throws Exception {
        OrdemDeServico primeiraOrdem = OrdemDeServico.abrir(
                "id-os-consulta-1",
                "OS-0001",
                new Funcionario("func-10", "Joao", null),
                new Cliente("cliente-os-10", "Marina", "12345678901"),
                new Veiculo("QRY1A23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        OrdemDeServico segundaOrdem = OrdemDeServico.abrir(
                "id-os-consulta-2",
                "OS-0002",
                new Funcionario("func-11", "Paulo", null),
                new Cliente("cliente-os-11", "Roberto", "99999999999"),
                new Veiculo("ZZZ9Z99", "Honda", "City", "Honda Motor Co.", 2023, 126, "AUTOMATICO", TipoCombustivel.FLEX));
        ordemDeServicoRepository.salvar(primeiraOrdem);
        ordemDeServicoRepository.salvar(segundaOrdem);

        mockMvc.perform(get("/ordens-servico")
                        .with(user("tester"))
                        .param("numeroOrdemServico", "OS-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroOrdemServico").value("OS-0001"))
                .andExpect(jsonPath("$[0].nomeCliente").value("Marina"));

        mockMvc.perform(get("/ordens-servico")
                        .with(user("tester"))
                        .param("nomeCliente", "Marina"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cpfCliente").value("12345678901"));

        mockMvc.perform(get("/ordens-servico")
                        .with(user("tester"))
                        .param("placaVeiculo", "QRY1A23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("id-os-consulta-1"));

        mockMvc.perform(get("/ordens-servico")
                        .with(user("tester"))
                        .param("cpfCliente", "12345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placaVeiculo").value("QRY1A23"));
    }

    @Test
    void deveIniciarDiagnostico() throws Exception {
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                "id-os-iniciar-1",
                "os-iniciar-1",
                new Funcionario("func-2", "Joao", null),
                new Cliente("cliente-os-2", "Ana"),
                new Veiculo("DEF2G34", "Honda", "Civic", "Honda Motor Co.", 2023, 155, "AUTOMATICO", TipoCombustivel.FLEX));
        ordemDeServicoRepository.salvar(ordemDeServico);

        mockMvc.perform(post("/ordens-servico/os-iniciar-1/diagnostico/iniciar")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveConcluirDiagnostico() throws Exception {
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                "id-os-concluir-1",
                "os-concluir-1",
                new Funcionario("func-3", "Carlos", null),
                new Cliente("cliente-os-3", "Paula"),
                new Veiculo("GHI3J45", "Fiat", "Argo", "Stellantis", 2022, 107, "MANUAL", TipoCombustivel.FLEX));
        ordemDeServico.iniciarDiagnostico();
        ordemDeServicoRepository.salvar(ordemDeServico);

        mockMvc.perform(post("/ordens-servico/os-concluir-1/diagnostico/concluir")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveFinalizarOrdemDeServico() throws Exception {
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                "id-os-finalizar-1",
                "os-finalizar-1",
                new Funcionario("func-4", "Marcos", null),
                new Cliente("cliente-os-4", "Bianca"),
                new Veiculo("JKL4M56", "Jeep", "Renegade", "Stellantis", 2024, 185, "AUTOMATICO", TipoCombustivel.DIESEL));
        ordemDeServico.iniciarDiagnostico();
        ordemDeServico.concluirDiagnostico();
        ordemDeServicoRepository.salvar(ordemDeServico);

        mockMvc.perform(post("/ordens-servico/os-finalizar-1/finalizacao")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        OrdemDeServico ordemAtualizada = ordemDeServicoRepository.buscarPorNumero("os-finalizar-1")
                .orElseThrow();
        assert ordemAtualizada.getStatus() == StatusOrdemDeServico.FINALIZADA;
    }
}
