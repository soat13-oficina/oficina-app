package br.com.oficina.ordemservico.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
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
        clienteRepository.salvar(new Cliente("cliente-os-1", "Maria"));
        veiculoRepository.salvar(new Veiculo("ABC1D23", "Toyota", "Corolla"));

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
    void deveIniciarDiagnostico() throws Exception {
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                "os-iniciar-1",
                new Funcionario("func-2", "Joao", null),
                new Cliente("cliente-os-2", "Ana"),
                new Veiculo("DEF2G34", "Honda", "Civic"));
        ordemDeServicoRepository.salvar(ordemDeServico);

        mockMvc.perform(post("/ordens-servico/os-iniciar-1/diagnostico/iniciar")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveConcluirDiagnostico() throws Exception {
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                "os-concluir-1",
                new Funcionario("func-3", "Carlos", null),
                new Cliente("cliente-os-3", "Paula"),
                new Veiculo("GHI3J45", "Fiat", "Argo"));
        ordemDeServico.iniciarDiagnostico();
        ordemDeServicoRepository.salvar(ordemDeServico);

        mockMvc.perform(post("/ordens-servico/os-concluir-1/diagnostico/concluir")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
