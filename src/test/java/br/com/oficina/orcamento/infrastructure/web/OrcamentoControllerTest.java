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

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.cliente.infrastructure.persistence.SpringDataClienteRepository;
import br.com.oficina.orcamento.infrastructure.persistence.SpringDataOrcamentoRepository;

@SpringBootTest
class OrcamentoControllerTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SpringDataClienteRepository clienteRepository;

    @Autowired
    private SpringDataOrcamentoRepository orcamentoRepository;

    private MockMvc mockMvc;
    private String clienteId;

    @BeforeEach
    void setUp() {
        orcamentoRepository.deleteAll();
        clienteRepository.deleteAll();
        clienteId = clienteRepository.save(new Cliente("Joao Silva", "12345678901", TipoCliente.PF)).getId().toString();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveExecutarCrudDeOrcamento() throws Exception {
        String cadastro = """
                {
                  "numeroOrcamento": "orc-1",
                  "clienteId": "%s",
                  "ordemDeServicoId": "os-1",
                  "funcionarioId": "func-1",
                  "placaVeiculo": "ABC1D23",
                  "marcaVeiculo": "Toyota",
                  "modeloVeiculo": "Corolla",
                  "descricaoDiagnostico": "Troca de pastilhas",
                  "servicosPropostos": ["Troca de pastilhas"],
                  "pecasPrevistas": ["Pastilha dianteira"],
                  "valorMaoDeObra": 150.00,
                  "valorPecas": 250.00,
                  "validade": "2030-12-31T10:00:00",
                  "observacoes": "Prioridade alta"
                }
                """.formatted(clienteId);

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
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.numeroOrcamento").value("orc-1"))
                .andExpect(jsonPath("$.cliente.nome").value("Joao Silva"))
                .andExpect(jsonPath("$.cliente.cpf").value("12345678901"))
                .andExpect(jsonPath("$.veiculo.placa").value("ABC1D23"))
                .andExpect(jsonPath("$.detalhesServico.valorTotal").value(400.0))
                .andExpect(jsonPath("$.detalhesServico.descricaoDiagnostico").value("Troca de pastilhas"))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"));

        String alteracao = """
                {
                  "clienteId": "%s",
                  "ordemDeServicoId": "os-1",
                  "funcionarioId": "func-2",
                  "placaVeiculo": "ABC1D23",
                  "marcaVeiculo": "Toyota",
                  "modeloVeiculo": "Corolla",
                  "descricaoDiagnostico": "Revisao de freios",
                  "servicosPropostos": ["Revisao freios"],
                  "pecasPrevistas": ["Fluido de freio"],
                  "valorMaoDeObra": 200.00,
                  "valorPecas": 100.00,
                  "validade": "2031-01-15T10:00:00",
                  "observacoes": "Aprovacao imediata"
                }
                """.formatted(clienteId);

        mockMvc.perform(put("/orcamentos/orc-1")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alteracao))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/orcamentos/orc-1")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalhesServico.valorTotal").value(300.0))
                .andExpect(jsonPath("$.detalhesServico.descricaoDiagnostico").value("Revisao de freios"));

        mockMvc.perform(get("/orcamentos")
                        .param("cpfCliente", "12345678901")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroOrcamento").value("orc-1"));

        mockMvc.perform(get("/orcamentos")
                        .param("placaVeiculo", "ABC1D23")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroOrcamento").value("orc-1"));

        mockMvc.perform(get("/orcamentos")
                        .param("numeroOrcamento", "orc-1")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroOrcamento").value("orc-1"));

        mockMvc.perform(delete("/orcamentos/orc-1")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/orcamentos/orc-1")
                        .with(user("tester")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornarNotFoundQuandoOrcamentoNaoExiste() throws Exception {
        mockMvc.perform(get("/orcamentos/orc-inexistente")
                        .with(user("tester")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Orcamento nao encontrado para o numero informado."));
    }

    @Test
    void deveRetornarBadRequestQuandoClienteIdForInvalido() throws Exception {
        String cadastro = """
                {
                  "numeroOrcamento": "orc-invalido",
                  "clienteId": "cliente-invalido",
                  "ordemDeServicoId": "os-1",
                  "funcionarioId": "func-1",
                  "placaVeiculo": "ABC1D23",
                  "marcaVeiculo": "Toyota",
                  "modeloVeiculo": "Corolla",
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Identificador do cliente invalido."));
    }

    @Test
    void deveRetornarBadRequestQuandoPayloadForInvalido() throws Exception {
        String cadastro = """
                {
                  "numeroOrcamento": "orc-payload-invalido",
                  "clienteId": "%s",
                  "ordemDeServicoId": "os-1",
                  "funcionarioId": "func-1",
                  "placaVeiculo": "ABC1D23",
                  "marcaVeiculo": "Toyota",
                  "modeloVeiculo": "Corolla",
                  "descricaoDiagnostico": "Troca de pastilhas",
                  "servicosPropostos": ["Troca de pastilhas"],
                  "pecasPrevistas": ["Pastilha dianteira"],
                  "valorMaoDeObra": 150.00,
                  "valorPecas": 250.00,
                  "validade": "data-invalida",
                  "observacoes": "Prioridade alta"
                }
                """.formatted(clienteId);

        mockMvc.perform(post("/orcamentos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Os dados enviados sao invalidos. Revise o corpo da requisicao."));
    }
}
