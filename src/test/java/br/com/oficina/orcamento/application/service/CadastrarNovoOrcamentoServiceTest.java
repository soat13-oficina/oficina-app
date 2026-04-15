package br.com.oficina.orcamento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.application.command.CadastrarNovoOrcamentoCommand;
import br.com.oficina.orcamento.domain.model.PecaOrcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.support.persistence.TestClienteRepository;
import br.com.oficina.support.persistence.TestOrcamentoRepository;

class CadastrarNovoOrcamentoServiceTest {

    @Test
    void deveCadastrarNovoOrcamento() {
        TestOrcamentoRepository repository = new TestOrcamentoRepository();
        TestClienteRepository clienteRepository = new TestClienteRepository();
        UUID clienteId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Joao Silva", "12345678901", TipoCliente.PF));
        CadastrarNovoOrcamentoService service = new CadastrarNovoOrcamentoService(repository, clienteRepository);

        service.cadastrarNovoOrcamento(new CadastrarNovoOrcamentoCommand(
                "orc-1",
                clienteId.toString(),
                "os-1",
                "func-1",
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Troca de pastilhas",
                List.of("Troca de pastilhas"),
                List.of(new PecaOrcamento("Pastilha dianteira", new BigDecimal("250.00"))),
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                LocalDateTime.of(2030, 1, 10, 10, 0),
                "Prioridade alta"));

        assertEquals(1, repository.buscarTodos().size());
        assertEquals(new BigDecimal("400.00"), repository.buscarPorNumeroOrcamento("orc-1").orElseThrow().getValorTotal());
        assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, repository.buscarPorNumeroOrcamento("orc-1").orElseThrow().getStatus());
        assertNotNull(repository.buscarPorNumeroOrcamento("orc-1").orElseThrow().getId());
        assertEquals(clienteId, repository.buscarPorNumeroOrcamento("orc-1").orElseThrow().getClienteId());
    }

    @Test
    void deveFalharQuandoNumeroOrcamentoJaExiste() {
        TestOrcamentoRepository repository = new TestOrcamentoRepository();
        TestClienteRepository clienteRepository = new TestClienteRepository();
        UUID clienteId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Joao Silva", "12345678901", TipoCliente.PF));
        repository.salvar(new br.com.oficina.orcamento.domain.model.Orcamento(
                "orc-1",
                clienteId,
                "os-1",
                "func-1",
                "Joao Silva",
                "12345678901",
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Troca de pastilhas",
                List.of("Troca de pastilhas"),
                List.of(new PecaOrcamento("Pastilha dianteira", new BigDecimal("250.00"))),
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 10, 10, 0),
                "Prioridade alta",
                StatusOrcamento.AGUARDANDO_APROVACAO));
        CadastrarNovoOrcamentoService service = new CadastrarNovoOrcamentoService(repository, clienteRepository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.cadastrarNovoOrcamento(new CadastrarNovoOrcamentoCommand(
                        "orc-1",
                        clienteId.toString(),
                        "os-2",
                        "func-2",
                        "XYZ9Z99",
                        "Honda",
                        "City",
                        "Revisao",
                        List.of("Revisao"),
                        List.of(new PecaOrcamento("Fluido", new BigDecimal("50.00"))),
                        new BigDecimal("100.00"),
                        BigDecimal.ZERO,
                        LocalDateTime.of(2030, 2, 1, 10, 0),
                        "Duplicado")));

        assertEquals("Ja existe orcamento cadastrado com o mesmo numero.", exception.getMessage());
    }

    @Test
    void deveFalharQuandoClienteNaoExiste() {
        CadastrarNovoOrcamentoService service = new CadastrarNovoOrcamentoService(
                new TestOrcamentoRepository(),
                new TestClienteRepository());

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.cadastrarNovoOrcamento(new CadastrarNovoOrcamentoCommand(
                        "orc-1",
                        UUID.fromString("99999999-9999-9999-9999-999999999999").toString(),
                        "os-1",
                        "func-1",
                        "ABC1D23",
                        "Toyota",
                        "Corolla",
                        "Troca de pastilhas",
                        List.of("Troca de pastilhas"),
                        List.of(new PecaOrcamento("Pastilha dianteira", new BigDecimal("250.00"))),
                        new BigDecimal("150.00"),
                        BigDecimal.ZERO,
                        LocalDateTime.of(2030, 1, 10, 10, 0),
                        "Prioridade alta")));

        assertEquals("Cliente nao encontrado para o identificador informado.", exception.getMessage());
    }
}
