package br.com.oficina.auth.infrastructure.controller;

import br.com.oficina.auth.domain.model.Usuario;
import br.com.oficina.auth.infrastructure.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // H2 começa vazio — inserimos o admin antes de cada teste
        usuarioRepository.deleteAll();
        usuarioRepository.save(Usuario.builder()
                .email("admin@oficina.com")
                .senha(passwordEncoder.encode("admin123"))
                .build());
    }

    @Test
    void deveRetornarRespostaFormatada_quandoCredenciaisCorretas() throws Exception {
        String body = """
                { "email": "admin@oficina.com", "senha": "admin123" }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)))
                // expiresAt deve ser string ISO-8601, nunca número epoch
                .andExpect(jsonPath("$.expiresAt").isString())
                .andExpect(jsonPath("$.expiresAt", matchesPattern("^\\d{4}-\\d{2}-\\d{2}T.*")))
                .andExpect(jsonPath("$.email").value("admin@oficina.com"));
    }

    @Test
    void deveRetornar401_quandoSenhaIncorreta() throws Exception {
        String body = """
                { "email": "admin@oficina.com", "senha": "errada" }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar401_quandoUsuarioNaoExiste() throws Exception {
        String body = """
                { "email": "naoexiste@oficina.com", "senha": "qualquer" }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar409_quandoRegistrarEmailJaCadastrado() throws Exception {
        String body = """
                { "email": "admin@oficina.com", "senha": "outrasenha" }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void deveRegistrarUsuarioERetornarRespostaFormatada_quandoEmailNovo() throws Exception {
        String body = """
                { "email": "novo@oficina.com", "senha": "senha123" }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                // mesmo contrato do login (FR-005)
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)))
                .andExpect(jsonPath("$.expiresAt").isString())
                .andExpect(jsonPath("$.expiresAt", matchesPattern("^\\d{4}-\\d{2}-\\d{2}T.*")))
                .andExpect(jsonPath("$.email").value("novo@oficina.com"));
    }

    @Test
    void naoDeveExporDadosSensiveis_nasRespostasDeAutenticacao() throws Exception {
        String login = """
                { "email": "admin@oficina.com", "senha": "admin123" }
                """;
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.hash").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());

        String registro = """
                { "email": "semvazamento@oficina.com", "senha": "senha123" }
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.hash").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void deveRetornar400_quandoLoginComEmailEmBranco() throws Exception {
        String body = """
                { "email": "", "senha": "admin123" }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400_quandoRegistrarComEmailSemFormatoValido() throws Exception {
        String body = """
                { "email": "abc", "senha": "senha123" }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400_quandoLoginComSenhaEmBranco() throws Exception {
        String body = """
                { "email": "admin@oficina.com", "senha": "" }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
