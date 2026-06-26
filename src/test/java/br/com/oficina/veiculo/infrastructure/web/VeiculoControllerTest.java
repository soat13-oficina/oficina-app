package br.com.oficina.veiculo.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
import br.com.oficina.cliente.infrastructure.persistence.SpringDataClienteRepository;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataFuncionarioRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;

@SpringBootTest
class VeiculoControllerTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SpringDataVeiculoRepository veiculoRepository;

    @Autowired
    private SpringDataClienteRepository clienteRepository;

    @Autowired
    private SpringDataOrdemDeServicoRepository ordemDeServicoRepository;

    @Autowired
    private SpringDataFuncionarioRepository funcionarioRepository;

    private MockMvc mockMvc;
    private String clienteId1;
    private String clienteId2;
    private String clienteId3;
    private String clienteId4;

    @BeforeEach
    void setUp() {
        ordemDeServicoRepository.deleteAll();
        funcionarioRepository.deleteAll();
        clienteRepository.deleteAll();
        veiculoRepository.deleteAll();
        clienteId1 = clienteRepository.save(new Cliente("Maria", "11111111111", TipoCliente.PF)).getId().toString();
        clienteId2 = clienteRepository.save(new Cliente("Joao", "22222222222", TipoCliente.PF)).getId().toString();
        clienteId3 = clienteRepository.save(new Cliente("Bianca", "33333333333", TipoCliente.PF)).getId().toString();
        clienteId4 = clienteRepository.save(new Cliente("Carlos", "44444444444", TipoCliente.PF)).getId().toString();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveCadastrarVeiculo() throws Exception {
        String requestBody = """
                {
                  "placa": "abc-1d23",
                  "marca": "Toyota",
                  "modelo": "Corolla",
                  "fabricante": "Toyota Motor Corporation",
                  "ano": 2024,
                  "potencia": 177,
                  "cambio": "AUTOMATICO",
                  "tipo": "FLEX",
                  "clienteId": "%s"
                }
                """.formatted(clienteId1);

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/veiculos/ABC1D23"));
    }

    @Test
    void deveConsultarVeiculosFiltrandoPorMarcaETipo() throws Exception {
        String corolla = """
                {
                  "placa": "ABC1D23",
                  "marca": "MarcaListaUnica",
                  "modelo": "Corolla",
                  "fabricante": "Fabricante Lista Unica",
                  "ano": 2024,
                  "potencia": 177,
                  "cambio": "AUTOMATICO",
                  "tipo": "FLEX",
                  "clienteId": "%s"
                }
                """.formatted(clienteId1);
        String tesla = """
                {
                  "placa": "TES1A23",
                  "marca": "Tesla",
                  "modelo": "Model 3",
                  "fabricante": "Tesla Inc.",
                  "ano": 2023,
                  "potencia": 283,
                  "cambio": "AUTOMATICO",
                  "tipo": "ELETRICO",
                  "clienteId": "%s"
                }
                """.formatted(clienteId2);

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corolla))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tesla))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/veiculos")
                        .param("marca", "MarcaListaUnica")
                        .param("tipo", "FLEX")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placa").value("ABC1D23"))
                .andExpect(jsonPath("$[0].fabricante").value("Fabricante Lista Unica"))
                .andExpect(jsonPath("$[0].ano").value(2024))
                .andExpect(jsonPath("$[0].potencia").value(177))
                .andExpect(jsonPath("$[0].cambio").value("AUTOMATICO"))
                .andExpect(jsonPath("$[0].tipo").value("FLEX"));
    }

    @Test
    void deveConsultarVeiculoPorPlacaMarcaOuFabricante() throws Exception {
        String consulta = """
                {
                  "placa": "QWE1A23",
                  "marca": "MarcaConsultaUnica",
                  "modelo": "Pulse",
                  "fabricante": "Fabricante Consulta Unica",
                  "ano": 2024,
                  "potencia": 130,
                  "cambio": "AUTOMATICO",
                  "tipo": "FLEX",
                  "clienteId": "%s"
                }
                """.formatted(clienteId1);

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(consulta))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/veiculos")
                        .param("placa", " qwe-1a23 ")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placa").value("QWE1A23"));

        mockMvc.perform(get("/veiculos")
                        .param("marca", "MarcaConsultaUnica")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placa").value("QWE1A23"));

        mockMvc.perform(get("/veiculos")
                        .param("fabricante", "Fabricante Consulta Unica")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placa").value("QWE1A23"));
    }

    @Test
    void deveExcluirVeiculo() throws Exception {
        String requestBody = """
                {
                  "placa": "DEL1E23",
                  "marca": "Ford",
                  "modelo": "Ranger",
                  "fabricante": "Ford Motor Company",
                  "ano": 2022,
                  "potencia": 213,
                  "cambio": "AUTOMATICO",
                  "tipo": "DIESEL",
                  "clienteId": "%s"
                }
                """.formatted(clienteId3);

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/veiculos/DEL1E23")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/veiculos")
                        .param("marca", "Ford")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deveAlterarVeiculo() throws Exception {
        String cadastro = """
                {
                  "placa": "ALT1E23",
                  "marca": "Volkswagen",
                  "modelo": "T-Cross",
                  "fabricante": "Volkswagen AG",
                  "ano": 2023,
                  "potencia": 128,
                  "cambio": "AUTOMATICO",
                  "tipo": "FLEX",
                  "clienteId": "%s"
                }
                """.formatted(clienteId4);
        String alteracao = """
                {
                  "marca": "Volkswagen",
                  "modelo": "Taos",
                  "fabricante": "Volkswagen AG",
                  "ano": 2024,
                  "potencia": 150,
                  "cambio": "AUTOMATICO",
                  "tipo": "GASOLINA"
                }
                """;

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/veiculos/ALT1E23")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alteracao))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/veiculos")
                        .param("marca", "Volkswagen")
                        .param("ano", "2024")
                        .param("tipo", "GASOLINA")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placa").value("ALT1E23"))
                .andExpect(jsonPath("$[0].modelo").value("Taos"))
                .andExpect(jsonPath("$[0].potencia").value(150))
                .andExpect(jsonPath("$[0].tipo").value("GASOLINA"));
    }

    @Test
    void deveRetornarNotFoundAoAlterarVeiculoInexistente() throws Exception {
        String alteracao = """
                {
                  "marca": "Volkswagen",
                  "modelo": "Taos",
                  "fabricante": "Volkswagen AG",
                  "ano": 2024,
                  "potencia": 150,
                  "cambio": "AUTOMATICO",
                  "tipo": "GASOLINA"
                }
                """;

        mockMvc.perform(put("/veiculos/ABC1D23")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alteracao))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veiculo nao encontrado para a placa informada."));
    }

    @Test
    void deveRetornarNotFoundAoExcluirVeiculoInexistente() throws Exception {
        mockMvc.perform(delete("/veiculos/ABC1D23")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Veiculo nao encontrado para a placa informada."));
    }

    @Test
    void deveRetornarBadRequestQuandoPlacaForInvalida() throws Exception {
        String requestBody = """
                {
                  "placa": "AB12",
                  "marca": "Toyota",
                  "modelo": "Corolla",
                  "fabricante": "Toyota Motor Corporation",
                  "ano": 2024,
                  "potencia": 177,
                  "cambio": "AUTOMATICO",
                  "tipo": "FLEX",
                  "clienteId": "%s"
                }
                """.formatted(clienteId1);

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Placa do veiculo invalida"));
    }

    @Test
    void deveRetornarBadRequestAoExcluirVeiculoComOrdemDeServicoVinculada() throws Exception {
        String requestBody = """
                {
                  "placa": "OSV1D23",
                  "marca": "Ford",
                  "modelo": "Ranger",
                  "fabricante": "Ford Motor Company",
                  "ano": 2022,
                  "potencia": 213,
                  "cambio": "AUTOMATICO",
                  "tipo": "DIESEL",
                  "clienteId": "%s"
                }
                """.formatted(clienteId1);

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        Veiculo veiculoSalvo = veiculoRepository.findByPlaca("OSV1D23").orElseThrow();
        Funcionario funcionario = funcionarioRepository.save(new Funcionario("Mecanico", "55555555555"));
        Cliente cliente = Cliente.reconstituir(UUID.fromString(clienteId1), "Maria", "11111111111", TipoCliente.PF);
        ordemDeServicoRepository.save(
                OrdemDeServico.abrir(null, "OS-WEB01", funcionario, cliente, veiculoSalvo));

        mockMvc.perform(delete("/veiculos/OSV1D23")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Nao e possivel excluir o veiculo: existem ordens de servico vinculadas."));

        mockMvc.perform(get("/veiculos")
                        .param("placa", "OSV1D23")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deveRetornarBadRequestQuandoPotenciaForInvalidaNoCadastro() throws Exception {
        String requestBody = """
                {
                  "placa": "POT1D23",
                  "marca": "Toyota",
                  "modelo": "Corolla",
                  "fabricante": "Toyota Motor Corporation",
                  "ano": 2024,
                  "potencia": 0,
                  "cambio": "AUTOMATICO",
                  "tipo": "FLEX",
                  "clienteId": "%s"
                }
                """.formatted(clienteId1);

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarBadRequestQuandoAnoForInvalidoNoCadastro() throws Exception {
        String requestBody = """
                {
                  "placa": "ANO1D23",
                  "marca": "Toyota",
                  "modelo": "Corolla",
                  "fabricante": "Toyota Motor Corporation",
                  "ano": 2999,
                  "potencia": 177,
                  "cambio": "AUTOMATICO",
                  "tipo": "FLEX",
                  "clienteId": "%s"
                }
                """.formatted(clienteId1);

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Ano do veiculo deve estar entre 1886 e")));
    }

    @Test
    void deveRetornarBadRequestQuandoPotenciaForInvalidaNaAlteracao() throws Exception {
        String cadastro = """
                {
                  "placa": "ALP1D23",
                  "marca": "Volkswagen",
                  "modelo": "T-Cross",
                  "fabricante": "Volkswagen AG",
                  "ano": 2023,
                  "potencia": 128,
                  "cambio": "AUTOMATICO",
                  "tipo": "FLEX",
                  "clienteId": "%s"
                }
                """.formatted(clienteId4);
        String alteracao = """
                {
                  "marca": "Volkswagen",
                  "modelo": "Taos",
                  "fabricante": "Volkswagen AG",
                  "ano": 2024,
                  "potencia": -5,
                  "cambio": "AUTOMATICO",
                  "tipo": "GASOLINA"
                }
                """;

        mockMvc.perform(post("/veiculos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/veiculos/ALP1D23")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alteracao))
                .andExpect(status().isBadRequest());
    }
}
