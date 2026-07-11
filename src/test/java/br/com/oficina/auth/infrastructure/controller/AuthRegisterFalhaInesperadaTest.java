package br.com.oficina.auth.infrastructure.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.com.oficina.auth.infrastructure.repository.UsuarioRepository;

/**
 * Isolado em classe própria (CRI-001): o {@link MockitoBean} substitui o {@link UsuarioRepository}
 * para simular uma falha inesperada de infraestrutura no registro, sem afetar os testes de caminho
 * feliz/401 de {@link AuthControllerTest} (que dependem do repositório real). Uso de Mockito aqui é
 * justificado por simular indisponibilidade de infraestrutura, não lógica de negócio.
 */
@SpringBootTest
class AuthRegisterFalhaInesperadaTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveRetornar500ComMensagemSegura_quandoFalhaInesperadaNoRegistro() throws Exception {
        when(usuarioRepository.findByEmail(any()))
                .thenThrow(new RuntimeException("Falha de banco simulada"));

        String body = """
                { "email": "falha@oficina.com", "senha": "senha123" }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro inesperado ao processar a requisicao."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Falha de banco simulada"))));
    }
}
