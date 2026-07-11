package br.com.oficina.auth.infrastructure.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import br.com.oficina.auth.infrastructure.repository.TokenIntegracaoRepository;
import br.com.oficina.auth.infrastructure.security.TokenIntegracaoService;

@SpringBootTest
class TokenIntegracaoControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TokenIntegracaoRepository repository;

    @Autowired
    private TokenIntegracaoService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void deveGerarTokenAutenticado() throws Exception {
        mockMvc.perform(post("/integracoes/tokens").with(user("tester")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rotulo\":\"ERP X\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.startsWith("oki_")))
                .andExpect(jsonPath("$.rotulo").value("ERP X"))
                .andExpect(jsonPath("$.criadoPor").value("tester"));
    }

    @Test
    void deveRecusarGeracaoSemAutenticacao() throws Exception {
        // Sem JWT a requisição é recusada (a app responde 403 — não há entry point custom para 401).
        mockMvc.perform(post("/integracoes/tokens").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rotulo\":\"ERP X\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarGeracaoSemRotulo() throws Exception {
        mockMvc.perform(post("/integracoes/tokens").with(user("tester")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarTokensSemExporSegredo() throws Exception {
        service.gerar("ERP X", "tester");

        mockMvc.perform(get("/integracoes/tokens").with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rotulo").value("ERP X"))
                .andExpect(jsonPath("$[0].status").value("ATIVO"))
                .andExpect(jsonPath("$[0].criadoPor").value("tester"))
                .andExpect(jsonPath("$[0].token").doesNotExist())
                .andExpect(jsonPath("$[0].hashToken").doesNotExist());
    }

    @Test
    void deveRevogarTokenEFalharParaInexistente() throws Exception {
        UUID id = service.gerar("ERP X", "tester").token().getId();

        mockMvc.perform(delete("/integracoes/tokens/" + id).with(user("tester")).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/integracoes/tokens/" + UUID.randomUUID()).with(user("tester")).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
