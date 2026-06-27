package br.com.oficina.orcamento.infrastructure.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.orcamento.infrastructure.persistence.SpringDataOrcamentoRepository;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataFuncionarioRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataOrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;

@SpringBootTest
class WebhookDecisaoOrcamentoControllerTest {

    private static final String TOKEN_VALIDO = "segredo-teste";
    private static final String HEADER_TOKEN = "X-Webhook-Token";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private OrdemDeServicoRepository ordemDeServicoRepository;

    @Autowired
    private SpringDataOrdemDeServicoRepository springDataOrdemDeServicoRepository;

    @Autowired
    private SpringDataOrcamentoRepository springDataOrcamentoRepository;

    @Autowired
    private SpringDataVeiculoRepository springDataVeiculoRepository;

    @Autowired
    private SpringDataClienteRepository springDataClienteRepository;

    @Autowired
    private SpringDataFuncionarioRepository springDataFuncionarioRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        springDataOrcamentoRepository.deleteAll();
        springDataOrdemDeServicoRepository.deleteAll();
        springDataVeiculoRepository.deleteAll();
        springDataClienteRepository.deleteAll();
        springDataFuncionarioRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveAprovarOrcamentoViaWebhookComTokenValido() throws Exception {
        OrdemDeServico ordem = criarOrdemEmAguardandoAprovacao();
        criarOrcamento("ORC-WHK-001", ordem, StatusOrcamento.AGUARDANDO_APROVACAO);

        mockMvc.perform(post("/integracoes/orcamentos/ORC-WHK-001/decisao")
                        .header(HEADER_TOKEN, TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "decisao": "APROVADO" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroOrcamento").value("ORC-WHK-001"))
                .andExpect(jsonPath("$.situacao").value("Execução"));
    }

    @Test
    void deveRejeitarOrcamentoViaWebhookComTokenValido() throws Exception {
        OrdemDeServico ordem = criarOrdemEmAguardandoAprovacao();
        criarOrcamento("ORC-WHK-002", ordem, StatusOrcamento.AGUARDANDO_APROVACAO);

        mockMvc.perform(post("/integracoes/orcamentos/ORC-WHK-002/decisao")
                        .header(HEADER_TOKEN, TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "decisao": "REJEITADO" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("Finalizada"));
    }

    @Test
    void deveRetornar401ComTokenInvalido() throws Exception {
        mockMvc.perform(post("/integracoes/orcamentos/ORC-001/decisao")
                        .header(HEADER_TOKEN, "token-errado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "decisao": "APROVADO" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(post("/integracoes/orcamentos/ORC-001/decisao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "decisao": "APROVADO" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar404ParaOrcamentoInexistente() throws Exception {
        mockMvc.perform(post("/integracoes/orcamentos/ORC-INEXISTENTE/decisao")
                        .header(HEADER_TOKEN, TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "decisao": "APROVADO" }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar409ComDecisaoDivergenteParaOrcamentoJaDecidido() throws Exception {
        OrdemDeServico ordem = criarOrdemEmAguardandoAprovacao();
        criarOrcamento("ORC-WHK-003", ordem, StatusOrcamento.AGUARDANDO_APROVACAO);

        mockMvc.perform(post("/integracoes/orcamentos/ORC-WHK-003/decisao")
                        .header(HEADER_TOKEN, TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "decisao": "APROVADO" }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/integracoes/orcamentos/ORC-WHK-003/decisao")
                        .header(HEADER_TOKEN, TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "decisao": "REJEITADO" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Decisao divergente da decisao ja registrada para o orcamento."));
    }

    @Test
    void deveRetornarMesmaDecisaoEmRetryIdenticoDeAprovacao() throws Exception {
        OrdemDeServico ordem = criarOrdemEmAguardandoAprovacao();
        criarOrcamento("ORC-WHK-004", ordem, StatusOrcamento.AGUARDANDO_APROVACAO);
        String payload = """
                { "decisao": "APROVADO" }
                """;

        mockMvc.perform(post("/integracoes/orcamentos/ORC-WHK-004/decisao")
                        .header(HEADER_TOKEN, TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/integracoes/orcamentos/ORC-WHK-004/decisao")
                        .header(HEADER_TOKEN, TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroOrcamento").value("ORC-WHK-004"));
    }

    private OrdemDeServico criarOrdemEmAguardandoAprovacao() {
        Cliente cliente = new Cliente("Maria", "11111111111", TipoCliente.PF);
        springDataClienteRepository.save(cliente);
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", null));
        Veiculo veiculo = springDataVeiculoRepository.save(new Veiculo(
                cliente.getId(), "WHK1D23", "Toyota", "Corolla", "Toyota Motor Corporation",
                2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));

        OrdemDeServico ordem = OrdemDeServico.reconstituir(
                null,
                "OS-WHK-" + UUID.randomUUID().toString().substring(0, 8),
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                Cliente.reconstituir(cliente.getId(), cliente.getNome(), cliente.getCpfOuCnpj(), TipoCliente.PF),
                veiculo,
                StatusOrdemDeServico.AGUARDANDO_APROVACAO,
                LocalDateTime.now(), null, null);
        ordemDeServicoRepository.salvar(ordem);
        return ordemDeServicoRepository.buscarTodas().stream()
                .filter(o -> o.getStatus() == StatusOrdemDeServico.AGUARDANDO_APROVACAO)
                .findFirst().orElseThrow();
    }

    private void criarOrcamento(String numero, OrdemDeServico ordem, StatusOrcamento status) {
        springDataOrcamentoRepository.save(new Orcamento(
                numero,
                ordem.getCliente().getId(),
                ordem.getId(),
                ordem.getFuncionario().getId(),
                ordem.getCliente().getNome(),
                ordem.getCliente().getCpfOuCnpj(),
                ordem.getVeiculo().getPlaca(),
                ordem.getVeiculo().getMarca(),
                ordem.getVeiculo().getModelo(),
                "Descricao diagnostico",
                List.of("Servico 1"),
                List.of(),
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                null,
                status));
    }
}
