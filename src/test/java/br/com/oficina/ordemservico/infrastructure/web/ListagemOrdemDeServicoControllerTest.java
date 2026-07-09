package br.com.oficina.ordemservico.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.cliente.infrastructure.persistence.SpringDataClienteRepository;
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
class ListagemOrdemDeServicoControllerTest {

    @Autowired
    private WebApplicationContext context;

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
    private Cliente cliente;
    private Funcionario funcionario;

    @BeforeEach
    void setUp() {
        springDataOrdemDeServicoRepository.deleteAll();
        springDataVeiculoRepository.deleteAll();
        springDataClienteRepository.deleteAll();
        springDataFuncionarioRepository.deleteAll();

        cliente = new Cliente("Maria", "20110101103", TipoCliente.PF);
        springDataClienteRepository.save(cliente);
        funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", null));

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deveListarOrdensAtivasPriorizadas() throws Exception {
        Veiculo v1 = salvarVeiculo("LST1A11");
        Veiculo v2 = salvarVeiculo("LST2B22");
        Veiculo v3 = salvarVeiculo("LST3C33");
        Veiculo v4 = salvarVeiculo("LST4D44");

        ordemDeServicoRepository.salvar(ordemReconstituida("OS-LST-RECV", StatusOrdemDeServico.OS_ABERTA, v1, LocalDateTime.now().minusHours(1), null));
        ordemDeServicoRepository.salvar(ordemReconstituida("OS-LST-DIAG", StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO, v2, LocalDateTime.now().minusHours(3), null));
        ordemDeServicoRepository.salvar(ordemReconstituida("OS-LST-APROV", StatusOrdemDeServico.AGUARDANDO_APROVACAO, v3, LocalDateTime.now().minusHours(2), null));
        ordemDeServicoRepository.salvar(ordemReconstituida("OS-LST-EXEC", StatusOrdemDeServico.SERVICO_EM_ANDAMENTO, v4, LocalDateTime.now().minusHours(4), null));

        mockMvc.perform(get("/ordens-servico").with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].numeroOrdemServico").value("OS-LST-EXEC"))
                .andExpect(jsonPath("$[1].numeroOrdemServico").value("OS-LST-APROV"))
                .andExpect(jsonPath("$[2].numeroOrdemServico").value("OS-LST-DIAG"))
                .andExpect(jsonPath("$[3].numeroOrdemServico").value("OS-LST-RECV"));
    }

    @Test
    void deveExcluirFinalizadasEEntreguesDoResultado() throws Exception {
        Veiculo v1 = salvarVeiculo("EXC1A11");
        Veiculo v2 = salvarVeiculo("EXC2B22");
        Veiculo v3 = salvarVeiculo("EXC3C33");

        ordemDeServicoRepository.salvar(ordemReconstituida("OS-ATIVA", StatusOrdemDeServico.OS_ABERTA, v1, null, null));
        ordemDeServicoRepository.salvar(ordemReconstituida("OS-FINAL", StatusOrdemDeServico.OS_FINALIZADA, v2,
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1)));
        ordemDeServicoRepository.salvar(ordemReconstituida("OS-ENTREGUE", StatusOrdemDeServico.ENTREGUE, v3,
                LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(2)));

        mockMvc.perform(get("/ordens-servico").with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numeroOrdemServico").value("OS-ATIVA"));
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaOrdensAtivas() throws Exception {
        mockMvc.perform(get("/ordens-servico").with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deveRetornarSituacaoCanonicaNoResponse() throws Exception {
        Veiculo v = salvarVeiculo("SIT1A11");
        ordemDeServicoRepository.salvar(ordemReconstituida("OS-SIT-001",
                StatusOrdemDeServico.AGUARDANDO_APROVACAO, v, LocalDateTime.now(), null));

        mockMvc.perform(get("/ordens-servico").with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].situacao").value("Aguardando Aprovação"));
    }

    private OrdemDeServico ordemReconstituida(
            String numero,
            StatusOrdemDeServico status,
            Veiculo veiculo,
            LocalDateTime iniciadaEm,
            LocalDateTime finalizadaEm) {
        return OrdemDeServico.reconstituir(
                null,
                numero,
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                Cliente.reconstituir(cliente.getId(), cliente.getNome(), cliente.getCpfOuCnpj(), TipoCliente.PF),
                veiculo,
                status,
                iniciadaEm,
                finalizadaEm,
                null);
    }

    private Veiculo salvarVeiculo(String placa) {
        return springDataVeiculoRepository.save(new Veiculo(
                cliente.getId(), placa, "Toyota", "Corolla", "Toyota Motor Corporation",
                2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
    }
}
