package br.com.oficina.orcamento.infrastructure.web;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class OrcamentoControllerTest {
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
    void deveExecutarCrudDeOrcamento() throws Exception {
        String cadastro = """
                {
                  "id": "orc-1",
                  "ordemDeServicoId": "os-1",
                  "funcionarioId": "func-1",
                  "clienteId": "cliente-1",
                  "placaVeiculo": "ABC1D23",
                  "descricaoDiagnostico": "Troca de pastilhas",
                  "servicosPropostos": ["Troca de pastilhas"],
                  "pecasPrevistas": ["Pastilha dianteira"],
                  "valorMaoDeObra": 150.00,
                  "valorPecas": 250.00,
                  "validade": "2030-12-31T10:00:00",
                  "observacoes": "Prioridade alta"
                }
                """;

        mockMvc.perform(post("/orcamentos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/orcamentos/orc-1"));

        mockMvc.perform(get("/orcamentos/orc-1")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("orc-1"))
                .andExpect(jsonPath("$.valorTotal").value(400.0))
                .andExpect(jsonPath("$.descricaoDiagnostico").value("Troca de pastilhas"));

        String alteracao = """
                {
                  "ordemDeServicoId": "os-1",
                  "funcionarioId": "func-2",
                  "clienteId": "cliente-1",
                  "placaVeiculo": "ABC1D23",
                  "descricaoDiagnostico": "Revisao de freios",
                  "servicosPropostos": ["Revisao freios"],
                  "pecasPrevistas": ["Fluido de freio"],
                  "valorMaoDeObra": 200.00,
                  "valorPecas": 100.00,
                  "validade": "2031-01-15T10:00:00",
                  "observacoes": "Aprovacao imediata"
                }
                """;

        mockMvc.perform(put("/orcamentos/orc-1")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alteracao))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/orcamentos/orc-1")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionarioId").value("func-2"))
                .andExpect(jsonPath("$.valorTotal").value(300.0))
                .andExpect(jsonPath("$.descricaoDiagnostico").value("Revisao de freios"));

        mockMvc.perform(delete("/orcamentos/orc-1")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/orcamentos/orc-1")
                        .with(user("tester")))
                .andExpect(status().isNotFound());
    }
}
