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
import br.com.oficina.orcamento.application.command.AlterarOrcamentoCommand;
import br.com.oficina.orcamento.application.command.PecaOrcamentoInput;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.PecaOrcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestClienteRepository;
import br.com.oficina.support.persistence.TestOrcamentoRepository;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

class AlterarOrcamentoServiceTest {

    private static final String PECA_ID = "peca-001";
    private static final String PECA_ID_2 = "peca-002";

    @Test
    void deveAlterarOrcamentoPreservandoCriadoEmEEnvio() {
        TestOrcamentoRepository repository = new TestOrcamentoRepository();
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestPecaInsumoRepository pecaInsumoRepository = new TestPecaInsumoRepository();
        UUID clienteId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID clienteAtualizadoId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Joao Silva", "12345678909", TipoCliente.PF));
        clienteRepository.salvar(Cliente.reconstituir(clienteAtualizadoId, "Maria Souza", "20100000053", TipoCliente.PF));
        pecaInsumoRepository.salvar(new PecaInsumo(PECA_ID, "Pastilha dianteira", "Bosch", new BigDecimal("250.00"), 10, 0, "REF-001", CategoriaPeca.FREIOS));
        pecaInsumoRepository.salvar(new PecaInsumo(PECA_ID_2, "Fluido de freio", "TRW", new BigDecimal("100.00"), 10, 0, "REF-002", CategoriaPeca.LUBRIFICANTES));
        Orcamento orcamento = novoOrcamento();
        orcamento.enviarParaAprovacao(LocalDateTime.of(2030, 1, 2, 9, 0));
        repository.salvar(orcamento);
        AlterarOrcamentoService service = new AlterarOrcamentoService(repository, clienteRepository, pecaInsumoRepository);

        service.alterarOrcamento(new AlterarOrcamentoCommand(
                "orc-1",
                clienteAtualizadoId.toString(),
                "77777777-7777-7777-7777-777777777777",
                "88888888-8888-8888-8888-888888888888",
                "XYZ9Z99",
                "Honda",
                "City",
                "Revisao de freios",
                List.of("Revisao freios"),
                List.of(new PecaOrcamentoInput(PECA_ID_2, 1)),
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                LocalDateTime.of(2031, 1, 10, 10, 0),
                "Aprovacao imediata"));

        Orcamento atualizado = repository.buscarPorNumeroOrcamento("orc-1").orElseThrow();
        assertNotNull(atualizado.getId());
        assertEquals(UUID.fromString("77777777-7777-7777-7777-777777777777"), atualizado.getOrdemDeServicoId());
        // Funcionario de origem e imutavel: a alteracao NAO troca o funcionario que abriu (FR-009/US4),
        // mesmo o comando informando 88888888.
        assertEquals(UUID.fromString("44444444-4444-4444-4444-444444444444"), atualizado.getFuncionarioId());
        assertEquals("Maria Souza", atualizado.getClienteNome());
        assertEquals("20100000053", atualizado.getClienteCpf());
        assertEquals(clienteAtualizadoId, atualizado.getClienteId());
        assertEquals("XYZ9Z99", atualizado.getPlacaVeiculo());
        assertEquals("Honda", atualizado.getMarcaVeiculo());
        assertEquals("City", atualizado.getModeloVeiculo());
        assertEquals(new BigDecimal("300.00"), atualizado.getValorTotal());
        assertEquals(LocalDateTime.of(2030, 1, 1, 10, 0), atualizado.getCriadoEm());
        assertEquals(LocalDateTime.of(2030, 1, 2, 9, 0), atualizado.getEnviadoParaAprovacaoEm());
        assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, atualizado.getStatus());
    }

    @Test
    void deveFalharAoAlterarOrcamentoInexistente() {
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestPecaInsumoRepository pecaInsumoRepository = new TestPecaInsumoRepository();
        UUID clienteId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Joao Silva", "12345678909", TipoCliente.PF));
        pecaInsumoRepository.salvar(new PecaInsumo(PECA_ID, "Pastilha dianteira", "Bosch", new BigDecimal("250.00"), 10, 0, "REF-001", CategoriaPeca.FREIOS));
        AlterarOrcamentoService service = new AlterarOrcamentoService(new TestOrcamentoRepository(), clienteRepository, pecaInsumoRepository);

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.alterarOrcamento(new AlterarOrcamentoCommand(
                        "orc-404",
                        clienteId.toString(),
                        "33333333-3333-3333-3333-333333333333",
                        "44444444-4444-4444-4444-444444444444",
                        "ABC1D23",
                        "Toyota",
                        "Corolla",
                        "Troca de pastilhas",
                        List.of("Troca de pastilhas"),
                        List.of(new PecaOrcamentoInput(PECA_ID, 1)),
                        new BigDecimal("150.00"),
                        BigDecimal.ZERO,
                        LocalDateTime.of(2030, 1, 10, 10, 0),
                        "Prioridade alta")));

        assertEquals("Orcamento nao encontrado para o numero informado.", exception.getMessage());
    }

    @Test
    void devePreservarStatusAprovadoAoAlterarOrcamento() {
        TestOrcamentoRepository repository = new TestOrcamentoRepository();
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestPecaInsumoRepository pecaInsumoRepository = new TestPecaInsumoRepository();
        UUID clienteId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Joao Silva", "12345678909", TipoCliente.PF));
        pecaInsumoRepository.salvar(new PecaInsumo(PECA_ID, "Peca", "Marca", new BigDecimal("50.00"), 10, 0, "REF", CategoriaPeca.FREIOS));
        Orcamento orcamento = novoOrcamento();
        orcamento.aprovar();
        repository.salvar(orcamento);
        AlterarOrcamentoService service = new AlterarOrcamentoService(repository, clienteRepository, pecaInsumoRepository);

        service.alterarOrcamento(new AlterarOrcamentoCommand(
                "orc-1",
                clienteId.toString(),
                "33333333-3333-3333-3333-333333333333",
                "44444444-4444-4444-4444-444444444444",
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Diagnostico aprovado",
                List.of("Servico"),
                List.of(new PecaOrcamentoInput(PECA_ID, 1)),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                LocalDateTime.of(2030, 1, 10, 10, 0),
                "Observacao"));

        assertEquals(StatusOrcamento.APROVADO, repository.buscarPorNumeroOrcamento("orc-1").orElseThrow().getStatus());
    }

    @Test
    void devePreservarStatusRejeitadoAoAlterarOrcamento() {
        TestOrcamentoRepository repository = new TestOrcamentoRepository();
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestPecaInsumoRepository pecaInsumoRepository = new TestPecaInsumoRepository();
        UUID clienteId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Joao Silva", "12345678909", TipoCliente.PF));
        pecaInsumoRepository.salvar(new PecaInsumo(PECA_ID, "Peca", "Marca", new BigDecimal("50.00"), 10, 0, "REF", CategoriaPeca.FREIOS));
        Orcamento orcamento = novoOrcamento();
        orcamento.rejeitar();
        repository.salvar(orcamento);
        AlterarOrcamentoService service = new AlterarOrcamentoService(repository, clienteRepository, pecaInsumoRepository);

        service.alterarOrcamento(new AlterarOrcamentoCommand(
                "orc-1",
                clienteId.toString(),
                "33333333-3333-3333-3333-333333333333",
                "44444444-4444-4444-4444-444444444444",
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Diagnostico rejeitado",
                List.of("Servico"),
                List.of(new PecaOrcamentoInput(PECA_ID, 1)),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                LocalDateTime.of(2030, 1, 10, 10, 0),
                "Observacao"));

        assertEquals(StatusOrcamento.REJEITADO, repository.buscarPorNumeroOrcamento("orc-1").orElseThrow().getStatus());
    }

    private Orcamento novoOrcamento() {
        return new Orcamento(
                "orc-1",
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "Joao Silva",
                "12345678909",
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Troca de pastilhas",
                List.of("Troca de pastilhas"),
                List.of(new PecaOrcamento(PECA_ID, "Pastilha dianteira", new BigDecimal("250.00"), 1)),
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 10, 10, 0),
                "Prioridade alta",
                StatusOrcamento.AGUARDANDO_APROVACAO);
    }
}
