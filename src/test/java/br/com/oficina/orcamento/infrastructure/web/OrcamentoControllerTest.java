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

import java.math.BigDecimal;

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
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.infrastructure.persistence.PecaInsumoJpaEntity;
import br.com.oficina.pecainsumo.infrastructure.persistence.SpringDataPecaInsumoRepository;

@SpringBootTest
class OrcamentoControllerTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SpringDataClienteRepository clienteRepository;

    @Autowired
    private SpringDataOrcamentoRepository orcamentoRepository;

    @Autowired
    private SpringDataPecaInsumoRepository pecaInsumoRepository;

    private MockMvc mockMvc;
    private String clienteId;
    private String pecaId1;
    private String pecaId2;

    @BeforeEach
    void setUp() {
        orcamentoRepository.deleteAll();
        pecaInsumoRepository.deleteAll();
        clienteRepository.deleteAll();
        clienteId = clienteRepository.save(new Cliente("Joao Silva", "12345678901", TipoCliente.PF)).getId().toString();

        PecaInsumo pastilha = new PecaInsumo("pastilha-001", "Pastilha dianteira", "Bosch", new BigDecimal("250.00"), 10, 0, "REF-PAST", CategoriaPeca.FREIOS);
        PecaInsumo fluido = new PecaInsumo("fluido-001", "Fluido de freio", "TRW", new BigDecimal("100.00"), 10, 0, "REF-FLUID", CategoriaPeca.FREIOS);
        pecaInsumoRepository.save(PecaInsumoJpaEntity.fromDomain(pastilha));
        pecaInsumoRepository.save(PecaInsumoJpaEntity.fromDomain(fluido));
        pecaId1 = pastilha.getId();
        pecaId2 = fluido.getId();

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
                  "ordemDeServicoId": "00000000-0000-0000-0000-000000000001",
                  "funcionarioId": "00000000-0000-0000-0000-000000000002",
                  "placaVeiculo": "ABC1D23",
                  "marcaVeiculo": "Toyota",
                  "modeloVeiculo": "Corolla",
                  "descricaoDiagnostico": "Troca de pastilhas",
                  "servicosPropostos": ["Troca de pastilhas"],
                  "pecasPrevistas": [{"pecaInsumoId": "%s", "quantidade": 1}],
                  "valorMaoDeObra": 150.00,
                  "desconto": 0.00,
                  "validade": "2030-12-31T10:00:00",
                  "observacoes": "Prioridade alta"
                }
                """.formatted(clienteId, pecaId1);

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
                  "ordemDeServicoId": "00000000-0000-0000-0000-000000000001",
                  "funcionarioId": "00000000-0000-0000-0000-000000000003",
                  "placaVeiculo": "ABC1D23",
                  "marcaVeiculo": "Toyota",
                  "modeloVeiculo": "Corolla",
                  "descricaoDiagnostico": "Revisao de freios",
                  "servicosPropostos": ["Revisao freios"],
                  "pecasPrevistas": [{"pecaInsumoId": "%s", "quantidade": 1}],
                  "valorMaoDeObra": 200.00,
                  "desconto": 0.00,
                  "validade": "2031-01-15T10:00:00",
                  "observacoes": "Aprovacao imediata"
                }
                """.formatted(clienteId, pecaId2);

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
                  "ordemDeServicoId": "00000000-0000-0000-0000-000000000001",
                  "funcionarioId": "00000000-0000-0000-0000-000000000002",
                  "placaVeiculo": "ABC1D23",
                  "marcaVeiculo": "Toyota",
                  "modeloVeiculo": "Corolla",
                  "descricaoDiagnostico": "Troca de pastilhas",
                  "servicosPropostos": ["Troca de pastilhas"],
                  "pecasPrevistas": [{"pecaInsumoId": "%s", "quantidade": 1}],
                  "valorMaoDeObra": 150.00,
                  "desconto": 0.00,
                  "validade": "2030-12-31T10:00:00",
                  "observacoes": "Prioridade alta"
                }
                """.formatted(pecaId1);

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
                  "ordemDeServicoId": "00000000-0000-0000-0000-000000000001",
                  "funcionarioId": "00000000-0000-0000-0000-000000000002",
                  "placaVeiculo": "ABC1D23",
                  "marcaVeiculo": "Toyota",
                  "modeloVeiculo": "Corolla",
                  "descricaoDiagnostico": "Troca de pastilhas",
                  "servicosPropostos": ["Troca de pastilhas"],
                  "pecasPrevistas": [{"pecaInsumoId": "%s", "quantidade": 1}],
                  "valorMaoDeObra": 150.00,
                  "desconto": 0.00,
                  "validade": "data-invalida",
                  "observacoes": "Prioridade alta"
                }
                """.formatted(clienteId, pecaId1);

        mockMvc.perform(post("/orcamentos")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastro))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Os dados enviados sao invalidos. Revise o corpo da requisicao."));
    }
}
