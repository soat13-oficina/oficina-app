package br.com.oficina.cliente.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.com.oficina.cliente.infrastructure.persistence.SpringDataClienteRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;

@SpringBootTest
class ClienteControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SpringDataClienteRepository clienteRepository;

    @Autowired
    private SpringDataVeiculoRepository veiculoRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        veiculoRepository.deleteAll();
        clienteRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveCadastrarEConsultarCliente() throws Exception {
        String requestBody = """
                {
                  "nome": "Maria",
                  "cpfOuCnpj": "12345678901",
                  "tipoCliente": "PF"
                }
                """;

        MvcResult cadastro = mockMvc.perform(post("/clientes")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/clientes/.+")))
                .andReturn();

        String location = cadastro.getResponse().getHeader("Location");
        String clienteId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/clientes/" + clienteId)
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clienteId))
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.cpfOuCnpj").value("12345678901"))
                .andExpect(jsonPath("$.tipoCliente").value("PF"));
    }

    @Test
    void devePesquisarClientesPorCpfENome() throws Exception {
        String nomeMaria = "Maria Silva " + UUID.randomUUID();
        String nomeJoao = "Joao Pereira " + UUID.randomUUID();

        mockMvc.perform(post("/clientes")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "%s",
                                  "cpfOuCnpj": "12345678901",
                                  "tipoCliente": "PF"
                                }
                                """.formatted(nomeMaria)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/clientes")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "%s",
                                  "cpfOuCnpj": "99999999999",
                                  "tipoCliente": "PF"
                                }
                                """.formatted(nomeJoao)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/clientes")
                        .with(user("tester"))
                        .param("termo", "123.456.789-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem(nomeMaria)))
                .andExpect(jsonPath("$[*].cpfOuCnpj", hasItem("12345678901")));

        mockMvc.perform(get("/clientes")
                        .with(user("tester"))
                        .param("termo", "Pereira"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem(nomeJoao)))
                .andExpect(jsonPath("$[*].cpfOuCnpj", hasItem("99999999999")));
    }

    @Test
    void deveRetornarNotFoundQuandoClienteNaoExiste() throws Exception {
        mockMvc.perform(get("/clientes/" + UUID.fromString("99999999-9999-9999-9999-999999999999"))
                        .with(user("tester")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente nao encontrado para o identificador informado."));
    }

    @Test
    void deveRetornarBadRequestQuandoIdentificadorForInvalido() throws Exception {
        mockMvc.perform(get("/clientes/cliente-invalido")
                        .with(user("tester")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Identificador do cliente invalido."));
    }

    @Test
    void deveRetornarBadRequestQuandoPayloadForInvalido() throws Exception {
        String requestBody = """
                {
                  "nome": "Maria",
                  "cpfOuCnpj": "12345678901",
                  "tipoCliente": "TIPO_INVALIDO"
                }
                """;

        mockMvc.perform(post("/clientes")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Os dados enviados sao invalidos. Revise o corpo da requisicao."));
    }

    @Test
    void deveRetornar409AoCadastrarClienteComDocumentoDuplicado() throws Exception {
        cadastrarClienteRetornandoId("Maria", "12345678901");

        mockMvc.perform(post("/clientes")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Ana",
                                  "cpfOuCnpj": "12345678901",
                                  "tipoCliente": "PF"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void deveExcluirClienteSemVinculos() throws Exception {
        String clienteId = cadastrarClienteRetornandoId("Maria", "12345678901");

        mockMvc.perform(delete("/clientes/" + clienteId)
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveRetornar404AoExcluirClienteInexistente() throws Exception {
        mockMvc.perform(delete("/clientes/" + UUID.fromString("99999999-9999-9999-9999-999999999999"))
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar409AoExcluirClienteComVeiculoVinculado() throws Exception {
        String clienteId = cadastrarClienteRetornandoId("Maria", "12345678901");
        veiculoRepository.save(new Veiculo(
                UUID.fromString(clienteId), "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation",
                2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));

        mockMvc.perform(delete("/clientes/" + clienteId)
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/clientes/" + clienteId)
                        .with(user("tester")))
                .andExpect(status().isOk());
    }

    private String cadastrarClienteRetornandoId(String nome, String cpfOuCnpj) throws Exception {
        MvcResult cadastro = mockMvc.perform(post("/clientes")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "%s",
                                  "cpfOuCnpj": "%s",
                                  "tipoCliente": "PF"
                                }
                                """.formatted(nome, cpfOuCnpj)))
                .andExpect(status().isCreated())
                .andReturn();
        String location = cadastro.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
