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
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataFuncionarioRepository;
import br.com.oficina.ordemservico.infrastructure.persistence.SpringDataOrdemDeServicoRepository;
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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        springDataOrdemDeServicoRepository.deleteAll();
        springDataVeiculoRepository.deleteAll();
        springDataClienteRepository.deleteAll();
        springDataFuncionarioRepository.deleteAll();
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
        Cliente cliente = clienteRepository.salvar(new Cliente("Bianca"));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Marcos", null));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "JKL4M56", "Jeep", "Renegade", "Stellantis", 2024, 185, "AUTOMATICO", TipoCombustivel.DIESEL));
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                null,
                "os-finalizar-1",
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                cliente,
                veiculoRepository.buscarPorPlaca("JKL4M56").orElseThrow());
        ordemDeServico.iniciarDiagnostico();
        ordemDeServico.concluirDiagnostico();
        ordemDeServicoRepository.salvar(ordemDeServico);

        mockMvc.perform(post("/ordens-servico/os-finalizar-1/finalizacao")
                        .with(user("tester"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        OrdemDeServico ordemAtualizada = ordemDeServicoRepository.buscarPorNumero("os-finalizar-1")
                .orElseThrow();
        assert ordemAtualizada.getStatus() == StatusOrdemDeServico.OS_FINALIZADA;
        assert ordemAtualizada.getFinalizadaEm() != null;
    }
}
