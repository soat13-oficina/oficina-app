package br.com.oficina.ordemservico.infrastructure.web;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataFuncionarioRepository;

@SpringBootTest
class FuncionarioControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SpringDataFuncionarioRepository funcionarioRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        funcionarioRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveCadastrarEConsultarFuncionario() throws Exception {
        MvcResult cadastro = mockMvc.perform(post("/funcionarios")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Joao Silva",
                                  "cpf": "12345678909"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("http://localhost/funcionarios/.+")))
                .andReturn();

        String location = cadastro.getResponse().getHeader("Location");
        String funcionarioId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/funcionarios/" + funcionarioId)
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(funcionarioId))
                .andExpect(jsonPath("$.nome").value("Joao Silva"))
                .andExpect(jsonPath("$.cpf").value("12345678909"));
    }

    @Test
    void deveCadastrarFuncionarioSemCpf() throws Exception {
        MvcResult cadastro = mockMvc.perform(post("/funcionarios")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Maria Souza"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String location = cadastro.getResponse().getHeader("Location");
        String funcionarioId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/funcionarios/" + funcionarioId)
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Souza"))
                .andExpect(jsonPath("$.cpf").doesNotExist());
    }

    @Test
    void deveListarFuncionariosComEFiltroNome() throws Exception {
        String nomeJoao = "Joao Controller " + UUID.randomUUID();
        String nomeMaria = "Maria Controller " + UUID.randomUUID();

        mockMvc.perform(post("/funcionarios")
                        .with(user("tester")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "%s", "cpf": "20140404430"}
                                """.formatted(nomeJoao)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/funcionarios")
                        .with(user("tester")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "%s", "cpf": "20150505582"}
                                """.formatted(nomeMaria)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/funcionarios").with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem(nomeJoao)))
                .andExpect(jsonPath("$[*].nome", hasItem(nomeMaria)));

        mockMvc.perform(get("/funcionarios").with(user("tester")).param("nome", "Joao Controller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem(nomeJoao)));
    }

    @Test
    void deveAlterarFuncionario() throws Exception {
        MvcResult cadastro = mockMvc.perform(post("/funcionarios")
                        .with(user("tester")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Nome Original", "cpf": "12345678909"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String location = cadastro.getResponse().getHeader("Location");
        String funcionarioId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(put("/funcionarios/" + funcionarioId)
                        .with(user("tester")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Nome Atualizado", "cpf": "20120202247"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/funcionarios/" + funcionarioId).with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Atualizado"))
                .andExpect(jsonPath("$.cpf").value("20120202247"));
    }

    @Test
    void deveExcluirFuncionario() throws Exception {
        MvcResult cadastro = mockMvc.perform(post("/funcionarios")
                        .with(user("tester")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Para Excluir", "cpf": "20140404430"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String location = cadastro.getResponse().getHeader("Location");
        String funcionarioId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(delete("/funcionarios/" + funcionarioId)
                        .with(user("tester")).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/funcionarios/" + funcionarioId).with(user("tester")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornarNotFoundQuandoFuncionarioNaoExistir() throws Exception {
        mockMvc.perform(get("/funcionarios/" + UUID.fromString("99999999-9999-9999-9999-999999999999"))
                        .with(user("tester")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Funcionario nao encontrado para o identificador informado."));
    }

    @Test
    void deveRetornarBadRequestQuandoIdentificadorForInvalido() throws Exception {
        mockMvc.perform(get("/funcionarios/identificador-invalido")
                        .with(user("tester")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Identificador do funcionario invalido."));
    }

    @Test
    void deveRetornarBadRequestAoCadastrarComCpfDuplicado() throws Exception {
        mockMvc.perform(post("/funcionarios")
                        .with(user("tester")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Primeiro Funcionario", "cpf": "12345678909"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/funcionarios")
                        .with(user("tester")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Segundo Funcionario", "cpf": "12345678909"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ja existe funcionario cadastrado com o mesmo CPF."));
    }
}
