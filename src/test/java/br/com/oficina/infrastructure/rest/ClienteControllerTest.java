package br.com.oficina.infrastructure.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
                  "id": "cliente-1",
                  "nome": "Maria"
                }
                """;

        mockMvc.perform(post("/clientes")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/clientes/cliente-1"));

        mockMvc.perform(get("/clientes/cliente-1")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cliente-1"))
                .andExpect(jsonPath("$.nome").value("Maria"));
    }

    @Test
    void deveRetornarNotFoundQuandoClienteNaoExiste() throws Exception {
        mockMvc.perform(get("/clientes/inexistente")
                        .with(user("tester")))
                .andExpect(status().isNotFound());
    }
}
