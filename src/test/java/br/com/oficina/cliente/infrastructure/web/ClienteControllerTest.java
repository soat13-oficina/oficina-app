package br.com.oficina.cliente.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ClienteControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
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
}
