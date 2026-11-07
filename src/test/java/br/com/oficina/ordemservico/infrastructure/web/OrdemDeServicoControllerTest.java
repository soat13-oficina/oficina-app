package br.com.oficina.ordemservico.infrastructure.web;

import static org.hamcrest.Matchers.not;
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
import br.com.oficina.support.saga.FluxoOficinaHttp;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;
import br.com.oficina.veiculo.infrastructure.persistence.SpringDataVeiculoRepository;
import java.time.LocalDateTime;

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
    private FluxoOficinaHttp fluxo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        fluxo = new FluxoOficinaHttp(mockMvc, context);
        fluxo.limpar();
    }

    @Test
    void deveCriarOrdemDeServico() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Maria", "20110101103", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", "12345678909"));
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroOrdemServico").exists())
                .andExpect(jsonPath("$.situacao").value("Recebida"));

        OrdemDeServico ordemCriada = ordemDeServicoRepository.buscarTodas().get(0);
        assert ordemCriada.getFuncionario().getId().equals(funcionario.getId());
        assert ordemCriada.getVeiculoId() != null;
    }

    @Test
    void deveConduzirCicloDeVidaEConsultarSituacao() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Maria", "20110101103", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", "12345678909"));
        veiculoRepository.salvar(new Veiculo(
                cliente.getId(),
                "CIC1D23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        String requestBody = """
                { "clienteId": "%s", "funcionarioId": "%s", "placaVeiculo": "CIC1D23" }
                """.formatted(cliente.getId(), funcionario.getId());
        mockMvc.perform(post("/ordens-servico").with(user("tester")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated());
        String numero = ordemDeServicoRepository.buscarTodas().get(0).getNumeroOrdemServico();

        mockMvc.perform(get("/ordens-servico/" + numero).with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("Recebida"));

        // Fase 4: a OS entra na fila de diagnostico da Execution; o mecanico trabalha la e a Saga reage aos eventos.
        fluxo.processarMensagens();
        fluxo.iniciarDiagnostico(numero);
        situacao(numero, "Diagnóstico");
        fluxo.concluirDiagnostico(numero, "Troca de oleo e revisao", null, 0);
        // DiagnosticoConcluido -> a Saga manda o GerarOrcamento -> OrcamentoGerado, sem ninguem "enviar para orcamento".
        situacao(numero, "Aguardando Aprovação");

        // O cliente aprova no Billing, que publica OrcamentoAprovado; a Saga reserva as pecas e pede o pagamento.
        String numeroOrcamento = fluxo.orcamento(numero).orElseThrow().getNumeroOrcamento();
        fluxo.clienteAprova(numeroOrcamento);
        situacao(numero, "Aguardando Aprovação");
        fluxo.processarMensagens();
        situacao(numero, "Aguardando Pagamento");

        fluxo.simularPagamento(numeroOrcamento, "APROVADO");
        fluxo.processarMensagens();
        situacao(numero, "Execução");

        fluxo.iniciarExecucao(numero);
        fluxo.concluirExecucao(numero);
        mockMvc.perform(get("/ordens-servico/" + numero).with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("Finalizada"))
                .andExpect(jsonPath("$.motivoEncerramento").value("SERVICO_CONCLUIDO"));
    }

    @Test
    void naoDeveExporMaisOsEndpointsDeDiagnosticoOrcamentoEExecucao() throws Exception {
        // Diagnostico e execucao sao da Execution (POST /execucoes/...) e o orcamento nasce sozinho do
        // DiagnosticoConcluido: o OS nao responde mais por nenhum desses caminhos.
        for (String caminho : new String[] {
                "/diagnostico/iniciar", "/diagnostico/concluir", "/diagnostico/enviar-para-orcamento",
                "/execucao/iniciar", "/servico/concluir", "/execucao/abortar"}) {
            mockMvc.perform(post("/ordens-servico/OS-QUALQUER" + caminho).with(user("tester")).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void naoDeveExporEndpointLegadoDeEnvioParaAprovacaoSemOrcamento() throws Exception {
        // O caminho legado foi removido: não deve mais responder com o 204 de sucesso de antes.
        mockMvc.perform(post("/ordens-servico/OS-QUALQUER/orcamento/enviar-aprovacao")
                .with(user("tester")).with(csrf()))
                .andExpect(status().is(not(204)));
    }

    private void situacao(String numero, String esperada) throws Exception {
        mockMvc.perform(get("/ordens-servico/" + numero).with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value(esperada));
    }

    @Test
    void deveRetornarBadRequestQuandoFuncionarioIdForInvalidoAoCriarOrdem() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Maria", "20110101103", TipoCliente.PF));
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
        Cliente cliente1 = clienteRepository.salvar(new Cliente("Marina", "12345678909", TipoCliente.PF));
        Cliente cliente2 = clienteRepository.salvar(new Cliente("Roberta", "20100000053", TipoCliente.PF));
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
        Cliente cliente = clienteRepository.salvar(new Cliente("Marina", "12345678909", TipoCliente.PF));
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
        Cliente cliente1 = clienteRepository.salvar(new Cliente("Marina", "12345678909", TipoCliente.PF));
        Cliente cliente2 = clienteRepository.salvar(new Cliente("Roberto", "20100000053", TipoCliente.PF));
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
                .andExpect(jsonPath("$[0].documentoCliente").value("12345678909"));

        mockMvc.perform(get("/ordens-servico")
                        .with(user("tester"))
                        .param("placaVeiculo", "QRY1A23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(primeiraOrdemId));

        mockMvc.perform(get("/ordens-servico")
                        .with(user("tester"))
                        .param("documentoCliente", "12345678909"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placaVeiculo").value("QRY1A23"))
                .andExpect(jsonPath("$[0].veiculoId").isNotEmpty());
    }

    @Test
    void deveConsultarTempoMedioDeExecucao() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Marina", "12345678909", TipoCliente.PF));
        Funcionario funcionario = springDataFuncionarioRepository.save(new Funcionario("Joao", null));
        Veiculo veiculo1 = springDataVeiculoRepository.save(new Veiculo(
                cliente.getId(),
                "MED1A23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        Veiculo veiculo2 = springDataVeiculoRepository.save(new Veiculo(
                cliente.getId(),
                "MED2B34", "Honda", "City", "Honda Motor Co.", 2023, 126, "AUTOMATICO", TipoCombustivel.FLEX));

        ordemDeServicoRepository.salvar(OrdemDeServico.reconstituir(
                null,
                "OS-MEDIA-001",
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                cliente,
                veiculo1,
                StatusOrdemDeServico.OS_FINALIZADA,
                LocalDateTime.of(2030, 1, 1, 8, 0),
                LocalDateTime.of(2030, 1, 1, 10, 0),
                null));
        ordemDeServicoRepository.salvar(OrdemDeServico.reconstituir(
                null,
                "OS-MEDIA-002",
                Funcionario.reconstituir(funcionario.getId(), funcionario.getNome(), funcionario.getCpf()),
                cliente,
                veiculo2,
                StatusOrdemDeServico.ENTREGUE,
                LocalDateTime.of(2030, 1, 1, 9, 0),
                LocalDateTime.of(2030, 1, 1, 12, 0),
                LocalDateTime.of(2030, 1, 1, 13, 0)));

        mockMvc.perform(get("/ordens-servico/metricas/tempo-medio")
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tempoMedioExecucaoEmSegundos").value(9000))
                .andExpect(jsonPath("$.tempoMedioExecucaoFormatado").value("2 horas e 30 minutos"))
                .andExpect(jsonPath("$.quantidadeOrdensConsideradas").value(2));
    }

    @Test
    void devePermitirClienteAcompanharAndamentoDaOrdem() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Marina", "12345678909", TipoCliente.PF));
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
                        .param("documentoCliente", "12345678909"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroOrdemServico").value("OS-CLIENTE-001"))
                .andExpect(jsonPath("$.nomeCliente").value("Marina"))
                .andExpect(jsonPath("$.placaVeiculo").value("CLI1A23"))
                .andExpect(jsonPath("$.status").value("DIAGNOSTICO_EM_ANDAMENTO"))
                .andExpect(jsonPath("$.iniciadaEm").isNotEmpty());
    }

    @Test
    void deveRetornarNotFoundQuandoClienteNaoPuderAcompanharOrdem() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Marina", "12345678909", TipoCliente.PF));
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
                        .param("documentoCliente", "20211112100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ordem de servico nao encontrada"));
    }

    @Test
    void deveEntregarOrdemDeServicoAoCliente() throws Exception {
        Cliente cliente = clienteRepository.salvar(new Cliente("Bianca", "20160606624", TipoCliente.PF));
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
        ordemDeServico.enviarParaAprovacao();
        ordemDeServico.recusarOrcamento();
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
        Cliente cliente = clienteRepository.salvar(new Cliente("Marina", "12345678909", TipoCliente.PF));
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
