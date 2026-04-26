package br.com.oficina.ordemservico.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.cliente.infrastructure.persistence.SpringDataClienteRepository;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.PecaOrcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.orcamento.infrastructure.persistence.SpringDataOrcamentoRepository;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataFuncionarioRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataOrdemDeServicoRepository;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.infrastructure.persistence.PecaInsumoJpaEntity;
import br.com.oficina.pecainsumo.infrastructure.persistence.SpringDataPecaInsumoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;

@SpringBootTest
class OrdemDeServicoControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    @Autowired
    private OrdemDeServicoRepository ordemDeServicoRepository;

    @Autowired
    private SpringDataOrdemDeServicoRepository springDataOrdemDeServicoRepository;

    @Autowired
    private SpringDataVeiculoRepository springDataVeiculoRepository;

    @Autowired
    private SpringDataClienteRepository springDataClienteRepository;

    @Autowired
    private SpringDataFuncionarioRepository springDataFuncionarioRepository;

    @Autowired
    private SpringDataOrcamentoRepository springDataOrcamentoRepository;

    @Autowired
    private SpringDataPecaInsumoRepository springDataPecaInsumoRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        springDataOrcamentoRepository.deleteAll();
        springDataOrdemDeServicoRepository.deleteAll();
        springDataVeiculoRepository.deleteAll();
        springDataClienteRepository.deleteAll();
        springDataFuncionarioRepository.deleteAll();
        springDataPecaInsumoRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveCriarOrdemDeServico() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Maria", "11111111111", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", "12345678901"));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));

        String requestBody = """
                {
                  "clienteId": "%s",
                  "funcionarioId": "%s",
                  "placaVeiculo": "ABC1D23"
                }
                """.formatted(cliente.getId(), funcionario.getId());

        mockMvc.perform(post("/ordens-servico")
                        .with(user("tester"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isAccepted());

        OrdemDeServico ordemCriada = ordemDeServicoRepository.buscarTodas().get(0);
        assert ordemCriada.getFuncionario().getId().equals(funcionario.getId());
        assert ordemCriada.getVeiculoId() != null;
    }

    @Test
    void deveRetornarBadRequestQuandoFuncionarioIdForInvalidoAoCriarOrdem() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Maria", "11111111111", TipoCliente.PF));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));

        String requestBody = """
                {
                  "clienteId": "%s",
                  "funcionarioId": "funcionario-invalido",
                  "placaVeiculo": "ABC1D23"
                }
                """.formatted(cliente.getId());

        mockMvc.perform(post("/ordens-servico")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Identificador do funcionario invalido."));
    }

    @Test
    void deveAlterarOrdemDeServico() throws Exception {
        Cliente cliente1 = clienteRepository.salvar(new Cliente("Marina", "12345678901", TipoCliente.PF));
        Cliente cliente2 = clienteRepository.salvar(new Cliente("Roberta", "99999999999", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", null));
        veiculoRepository.salvar(new Veiculo(
                cliente1.getId(),
                "ALT1A11", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        veiculoRepository.salvar(new Veiculo(
                cliente2.getId(),
                "ALT2B22", "Honda", "City", "Honda Motor Co.", 2023, 126, "AUTOMATICO", TipoCombustivel.FLEX));
        Veiculo veiculoCliente1 = veiculoRepository.buscarPorPlaca("ALT1A11").orElseThrow();
        Veiculo veiculoCliente2 = veiculoRepository.buscarPorPlaca("ALT2B22").orElseThrow();

        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                null,
                "OS-ALTERAR-1",
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                cliente1,
                veiculoCliente1);
        ordemDeServicoRepository.salvar(ordemDeServico);

        String requestBody = """
                {
                  "clienteId": "%s",
                  "funcionarioId": "%s",
                  "placaVeiculo": "ALT2B22"
                }
                """.formatted(cliente2.getId(), funcionario.getId());

        mockMvc.perform(put("/ordens-servico/OS-ALTERAR-1")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        OrdemDeServico ordemAtualizada = ordemDeServicoRepository.buscarPorNumero("OS-ALTERAR-1")
                .orElseThrow();
        assert ordemAtualizada.getCliente().getId().equals(cliente2.getId());
        assert ordemAtualizada.getVeiculo().getPlaca().equals("ALT2B22");
        assert ordemAtualizada.getFuncionario().getId().equals(funcionario.getId());
        assert ordemAtualizada.getVeiculoId().equals(veiculoCliente2.getId());
    }

    @Test
    void deveExcluirOrdemDeServico() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Marina", "12345678901", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", null));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "DEL1O23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                null,
                "OS-EXCLUIR-1",
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                cliente,
                veiculoRepository.buscarPorPlaca("DEL1O23").orElseThrow());
        ordemDeServicoRepository.salvar(ordemDeServico);

        mockMvc.perform(delete("/ordens-servico/OS-EXCLUIR-1")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assert ordemDeServicoRepository.buscarPorNumero("OS-EXCLUIR-1").isEmpty();
    }

    @Test
    void deveConsultarOrdemDeServicoPorFiltros() throws Exception {
        Cliente cliente1 = clienteRepository.salvar(new Cliente("Marina", "12345678901", TipoCliente.PF));
        Cliente cliente2 = clienteRepository.salvar(new Cliente("Roberto", "99999999999", TipoCliente.PF));
        Funcionario funcionario1 = springDataFuncionarioRepository.save(new Funcionario("Joao", null));
        Funcionario funcionario2 = springDataFuncionarioRepository.save(new Funcionario("Paulo", null));
        veiculoRepository.salvar(new Veiculo(
                cliente1.getId(),
                "QRY1A23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        veiculoRepository.salvar(new Veiculo(
                cliente2.getId(),
                "ZZZ9Z99", "Honda", "City", "Honda Motor Co.", 2023, 126, "AUTOMATICO", TipoCombustivel.FLEX));
        OrdemDeServico primeiraOrdem = OrdemDeServico.abrir(
                null,
                "OS-0001",
                Funcionario.reconstituir(funcionario1.getId(), funcionario1.getNome(), funcionario1.getCpf()),
                cliente1,
                veiculoRepository.buscarPorPlaca("QRY1A23").orElseThrow());
        OrdemDeServico segundaOrdem = OrdemDeServico.abrir(
                null,
                "OS-0002",
                Funcionario.reconstituir(funcionario2.getId(), funcionario2.getNome(), funcionario2.getCpf()),
                cliente2,
                veiculoRepository.buscarPorPlaca("ZZZ9Z99").orElseThrow());
        ordemDeServicoRepository.salvar(primeiraOrdem);
        ordemDeServicoRepository.salvar(segundaOrdem);
        String primeiraOrdemId = ordemDeServicoRepository.buscarPorNumero("OS-0001").orElseThrow().getId().toString();

        mockMvc.perform(get("/ordens-servico")
                        .with(user("tester"))
                        .param("numeroOrdemServico", "OS-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroOrdemServico").value("OS-0001"))
                .andExpect(jsonPath("$[0].nomeCliente").value("Marina"))
                .andExpect(jsonPath("$[0].funcionarioId").value(funcionario1.getId().toString()));

        mockMvc.perform(get("/ordens-servico")
                        .with(user("tester"))
                        .param("nomeCliente", "Marina"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentoCliente").value("12345678901"));

        mockMvc.perform(get("/ordens-servico")
                        .with(user("tester"))
                        .param("placaVeiculo", "QRY1A23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(primeiraOrdemId));

        mockMvc.perform(get("/ordens-servico")
                        .with(user("tester"))
                        .param("documentoCliente", "12345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placaVeiculo").value("QRY1A23"))
                .andExpect(jsonPath("$[0].veiculoId").isNotEmpty());
    }

    @Test
    void devePermitirClienteAcompanharAndamentoDaOrdem() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Marina", "12345678901", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", null));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "CLI1A23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                null,
                "OS-CLIENTE-001",
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                cliente,
                veiculoRepository.buscarPorPlaca("CLI1A23").orElseThrow());
        ordemDeServico.iniciarDiagnostico();
        ordemDeServicoRepository.salvar(ordemDeServico);

        mockMvc.perform(get("/ordens-servico/OS-CLIENTE-001/acompanhamento")
                        .with(user("tester"))
                        .param("documentoCliente", "12345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroOrdemServico").value("OS-CLIENTE-001"))
                .andExpect(jsonPath("$.nomeCliente").value("Marina"))
                .andExpect(jsonPath("$.placaVeiculo").value("CLI1A23"))
                .andExpect(jsonPath("$.status").value("DIAGNOSTICO_EM_ANDAMENTO"))
                .andExpect(jsonPath("$.iniciadaEm").isNotEmpty());
    }

    @Test
    void deveRetornarNotFoundQuandoClienteNaoPuderAcompanharOrdem() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Marina", "12345678901", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", null));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "CLI9Z99", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        ordemDeServicoRepository.salvar(OrdemDeServico.abrir(
                null,
                "OS-CLIENTE-002",
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                cliente,
                veiculoRepository.buscarPorPlaca("CLI9Z99").orElseThrow()));

        mockMvc.perform(get("/ordens-servico/OS-CLIENTE-002/acompanhamento")
                        .with(user("tester"))
                        .param("documentoCliente", "00000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ordem de servico nao encontrada"));
    }

    @Test
    void deveIniciarDiagnostico() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Ana"));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", null));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "DEF2G34", "Honda", "Civic", "Honda Motor Co.", 2023, 155, "AUTOMATICO", TipoCombustivel.FLEX));
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                null,
                "os-iniciar-1",
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                cliente,
                veiculoRepository.buscarPorPlaca("DEF2G34").orElseThrow());
        ordemDeServicoRepository.salvar(ordemDeServico);

        mockMvc.perform(post("/ordens-servico/os-iniciar-1/diagnostico/iniciar")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        OrdemDeServico ordemAtualizada = ordemDeServicoRepository.buscarPorNumero("os-iniciar-1").orElseThrow();
        assert ordemAtualizada.getIniciadaEm() != null;
    }

    @Test
    void deveConcluirDiagnostico() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Paula"));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Carlos", null));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "GHI3J45", "Fiat", "Argo", "Stellantis", 2022, 107, "MANUAL", TipoCombustivel.FLEX));
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                null,
                "os-concluir-1",
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                cliente,
                veiculoRepository.buscarPorPlaca("GHI3J45").orElseThrow());
        ordemDeServico.iniciarDiagnostico();
        ordemDeServicoRepository.salvar(ordemDeServico);

        mockMvc.perform(post("/ordens-servico/os-concluir-1/diagnostico/concluir")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveFinalizarOrdemDeServico() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Bianca", "55555555555", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Marcos", null));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "JKL4M56", "Jeep", "Renegade", "Stellantis", 2024, 185, "AUTOMATICO", TipoCombustivel.DIESEL));

        // Create a PecaInsumo in the database with reserved stock (simulating prior reservation from approval)
        String pecaId = "filtro-oleo-001";
        PecaInsumo pecaInsumo = new PecaInsumo(pecaId, "Filtro de oleo", "Bosch", new java.math.BigDecimal("80.00"), 10, 1, "REF-FILTRO", CategoriaPeca.FILTROS);
        springDataPecaInsumoRepository.save(PecaInsumoJpaEntity.fromDomain(pecaInsumo));

        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                null,
                "os-finalizar-1",
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                cliente,
                veiculoRepository.buscarPorPlaca("JKL4M56").orElseThrow());
        ordemDeServico.iniciarDiagnostico();
        ordemDeServico.concluirDiagnostico();
        ordemDeServico.enviarParaOrcamento();
        ordemDeServicoRepository.salvar(ordemDeServico);
        springDataOrcamentoRepository.save(new Orcamento(
                "ORC-001",
                cliente.getId(),
                ordemDeServico.getId(),
                funcionario.getId(),
                cliente.getNome(),
                cliente.getCpfOuCnpj(),
                "JKL4M56",
                "Jeep",
                "Renegade",
                "Troca de oleo",
                java.util.List.of("Troca de oleo"),
                java.util.List.of(new PecaOrcamento(pecaId, "Filtro de oleo", new java.math.BigDecimal("80.00"), 1)),
                new java.math.BigDecimal("200.00"),
                java.math.BigDecimal.ZERO,
                java.time.LocalDateTime.of(2030, 1, 1, 10, 0),
                java.time.LocalDateTime.of(2030, 1, 10, 10, 0),
                "Finalizacao",
                StatusOrcamento.AGUARDANDO_APROVACAO));

        mockMvc.perform(post("/ordens-servico/os-finalizar-1/finalizacao")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroOrdemServico").value("os-finalizar-1"))
                .andExpect(jsonPath("$.cliente.nome").value("Bianca"))
                .andExpect(jsonPath("$.veiculo.placa").value("JKL4M56"))
                .andExpect(jsonPath("$.servicoRealizado").value("Troca de oleo"))
                .andExpect(jsonPath("$.valorServico").value(200.0))
                .andExpect(jsonPath("$.pecas[0].descricao").value("Filtro de oleo"))
                .andExpect(jsonPath("$.pecas[0].preco").value(80.0))
                .andExpect(jsonPath("$.valorFinal").value(280.0))
                .andExpect(jsonPath("$.desconto").value(0));

        OrdemDeServico ordemAtualizada = ordemDeServicoRepository.buscarPorNumero("os-finalizar-1")
                .orElseThrow();
        assert ordemAtualizada.getStatus() == StatusOrdemDeServico.OS_FINALIZADA;
        assert ordemAtualizada.getFinalizadaEm() != null;
    }

    @Test
    void deveEntregarOrdemDeServicoAoCliente() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Bianca", "55555555555", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Marcos", null));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "ENT4G56", "Jeep", "Renegade", "Stellantis", 2024, 185, "AUTOMATICO", TipoCombustivel.DIESEL));

        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                null,
                "os-entregar-1",
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                cliente,
                veiculoRepository.buscarPorPlaca("ENT4G56").orElseThrow());
        ordemDeServico.iniciarDiagnostico();
        ordemDeServico.concluirDiagnostico();
        ordemDeServico.enviarParaOrcamento();
        ordemDeServico.finalizar();
        ordemDeServicoRepository.salvar(ordemDeServico);

        mockMvc.perform(post("/ordens-servico/os-entregar-1/entrega")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        OrdemDeServico ordemAtualizada = ordemDeServicoRepository.buscarPorNumero("os-entregar-1")
                .orElseThrow();
        assert ordemAtualizada.getStatus() == StatusOrdemDeServico.ENTREGUE;
        assert ordemAtualizada.getEntregueEm() != null;
    }

    @Test
    void deveRetornarNotFoundQuandoOrdemNaoExistirAoExcluir() throws Exception {
        mockMvc.perform(delete("/ordens-servico/OS-404")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ordem de servico nao encontrada para o numero informado."));
    }

    @Test
    void deveRetornarNotFoundQuandoFuncionarioNaoExistirAoAlterarOrdem() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Marina", "12345678901", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", null));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "ALT1A11", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                null,
                "OS-ALTERAR-404",
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                cliente,
                veiculoRepository.buscarPorPlaca("ALT1A11").orElseThrow());
        ordemDeServicoRepository.salvar(ordemDeServico);

        String requestBody = """
                {
                  "clienteId": "%s",
                  "funcionarioId": "99999999-9999-9999-9999-999999999999",
                  "placaVeiculo": "ALT1A11"
                }
                """.formatted(cliente.getId());

        mockMvc.perform(put("/ordens-servico/OS-ALTERAR-404")
                        .with(user("tester"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Funcionario nao encontrado para o identificador informado."));
    }
}
