package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.application.usecase.CadastrarNovoOrcamentoUseCase;
import br.com.oficina.orcamento.application.service.CadastrarNovoOrcamentoService;
import br.com.oficina.ordemservico.application.usecase.EnviarDiagnosticoParaOrcamentoUseCase.EnviarDiagnosticoParaOrcamentoRequest;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;
import br.com.oficina.support.persistence.TestClienteRepository;
import br.com.oficina.support.persistence.TestOrcamentoRepository;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class EnviarDiagnosticoParaOrcamentoServiceTest {

    private static final UUID CLIENTE_ID = UUID.fromString("61111111-1111-1111-1111-111111111111");

    @Test
    void deveCriarOrcamentoTransicionarEPublicarEvento() {
        TestOrdemDeServicoRepository ordemRepository = new TestOrdemDeServicoRepository();
        TestOrcamentoRepository orcamentoRepository = new TestOrcamentoRepository();
        List<Object> eventos = new ArrayList<>();
        prepararOrdemEmDiagnosticoConcluido(ordemRepository, "OS-001");

        EnviarDiagnosticoParaOrcamentoService service = new EnviarDiagnosticoParaOrcamentoService(
                ordemRepository,
                cadastrarOrcamentoReal(orcamentoRepository),
                eventos::add);

        service.enviarDiagnosticoParaOrcamento(requisicao("OS-001"));

        OrdemDeServico ordem = ordemRepository.buscarPorNumero("OS-001").orElseThrow();
        assertEquals(StatusOrdemDeServico.AGUARDANDO_APROVACAO, ordem.getStatus());
        assertEquals(1, orcamentoRepository.buscarTodos().size());
        assertEquals(1, eventos.size());
        assertTrue(eventos.get(0) instanceof StatusOrdemDeServicoAlterado);
    }

    @Test
    void deveFazerRollbackENaoPublicarEventoQuandoCriacaoDeOrcamentoFalha() {
        // Em nível unitário, sem gerenciador de transações, validamos que a falha propaga,
        // nenhum orçamento é persistido e o evento NÃO é publicado. O rollback efetivo do
        // status da OS no banco é garantido por @Transactional (verificado em integração).
        TestOrdemDeServicoRepository ordemRepository = new TestOrdemDeServicoRepository();
        TestOrcamentoRepository orcamentoRepository = new TestOrcamentoRepository();
        List<Object> eventos = new ArrayList<>();
        prepararOrdemEmDiagnosticoConcluido(ordemRepository, "OS-002");

        CadastrarNovoOrcamentoUseCase cadastroQueFalha = command -> {
            throw new RegraDeNegocioException("Falha simulada na criacao do orcamento.");
        };
        EnviarDiagnosticoParaOrcamentoService service = new EnviarDiagnosticoParaOrcamentoService(
                ordemRepository, cadastroQueFalha, eventos::add);

        assertThrows(RegraDeNegocioException.class, () -> service.enviarDiagnosticoParaOrcamento(requisicao("OS-002")));

        assertTrue(orcamentoRepository.buscarTodos().isEmpty());
        assertTrue(eventos.isEmpty());
    }

    @Test
    void deveLancarRecursoNaoEncontradoQuandoOrdemNaoExiste() {
        TestOrdemDeServicoRepository ordemRepository = new TestOrdemDeServicoRepository();
        TestOrcamentoRepository orcamentoRepository = new TestOrcamentoRepository();
        EnviarDiagnosticoParaOrcamentoService service = new EnviarDiagnosticoParaOrcamentoService(
                ordemRepository, cadastrarOrcamentoReal(orcamentoRepository), event -> {});

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.enviarDiagnosticoParaOrcamento(requisicao("OS-404")));
    }

    @Test
    void deveRecusarSegundaTransicaoConcorrenteSemCriarSegundoOrcamento() {
        TestOrdemDeServicoRepository ordemRepository = new TestOrdemDeServicoRepository();
        TestOrcamentoRepository orcamentoRepository = new TestOrcamentoRepository();
        prepararOrdemEmDiagnosticoConcluido(ordemRepository, "OS-003");
        EnviarDiagnosticoParaOrcamentoService service = new EnviarDiagnosticoParaOrcamentoService(
                ordemRepository, cadastrarOrcamentoReal(orcamentoRepository), event -> {});

        service.enviarDiagnosticoParaOrcamento(requisicao("OS-003"));

        // Segunda chamada para a mesma OS (já em AGUARDANDO_APROVACAO) é recusada pela
        // precondição de situação, sem criar um segundo orçamento (FR-006 / research D4).
        assertThrows(RegraDeNegocioException.class,
                () -> service.enviarDiagnosticoParaOrcamento(requisicao("OS-003")));
        assertEquals(1, orcamentoRepository.buscarTodos().size());
    }

    @Test
    void deveRecusarQuandoDescontoTornaTotalNegativo() {
        TestOrdemDeServicoRepository ordemRepository = new TestOrdemDeServicoRepository();
        TestOrcamentoRepository orcamentoRepository = new TestOrcamentoRepository();
        prepararOrdemEmDiagnosticoConcluido(ordemRepository, "OS-005");
        EnviarDiagnosticoParaOrcamentoService service = new EnviarDiagnosticoParaOrcamentoService(
                ordemRepository, cadastrarOrcamentoReal(orcamentoRepository), event -> {});

        // Sem peças: total = maoDeObra - desconto; desconto > maoDeObra -> total negativo -> recusa (F3).
        EnviarDiagnosticoParaOrcamentoRequest req = new EnviarDiagnosticoParaOrcamentoRequest(
                "OS-005",
                new BigDecimal("100.00"),
                new BigDecimal("500.00"),
                LocalDateTime.of(2030, 1, 1, 0, 0),
                null);
        assertThrows(RegraDeNegocioException.class, () -> service.enviarDiagnosticoParaOrcamento(req));
        assertTrue(orcamentoRepository.buscarTodos().isEmpty());
    }

    private CadastrarNovoOrcamentoUseCase cadastrarOrcamentoReal(TestOrcamentoRepository orcamentoRepository) {
        TestClienteRepository clienteRepository = new TestClienteRepository();
        clienteRepository.salvar(Cliente.reconstituir(CLIENTE_ID, "Maria", "20110101103", TipoCliente.PF));
        return new CadastrarNovoOrcamentoService(orcamentoRepository, clienteRepository, new TestPecaInsumoRepository());
    }

    private void prepararOrdemEmDiagnosticoConcluido(TestOrdemDeServicoRepository repository, String numero) {
        OrdemDeServico ordem = novaOrdem(numero);
        ordem.iniciarDiagnostico();
        // Diagnóstico fechado com descrição do serviço (fonte do orçamento); peças vazias neste cenário.
        ordem.concluirDiagnostico("Servico de diagnostico", List.of());
        repository.salvar(ordem);
    }

    private EnviarDiagnosticoParaOrcamentoRequest requisicao(String numero) {
        return new EnviarDiagnosticoParaOrcamentoRequest(
                numero,
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                LocalDateTime.of(2030, 1, 1, 0, 0),
                null);
    }

    private OrdemDeServico novaOrdem(String numero) {
        UUID funcionarioId = UUID.fromString("71111111-1111-1111-1111-111111111111");
        return OrdemDeServico.abrir(
                UUID.nameUUIDFromBytes(("ordem-" + numero).getBytes()),
                numero,
                Funcionario.reconstituir(funcionarioId, "Joao", null),
                Cliente.reconstituir(CLIENTE_ID, "Maria", "20110101103", TipoCliente.PF),
                Veiculo.reconstituir(
                        UUID.nameUUIDFromBytes(("veiculo-" + numero).getBytes()),
                        CLIENTE_ID,
                        "ABC1D23",
                        "Toyota",
                        "Corolla",
                        "Toyota Motor Corporation",
                        2024,
                        177,
                        "AUTOMATICO",
                        TipoCombustivel.FLEX));
    }
}
