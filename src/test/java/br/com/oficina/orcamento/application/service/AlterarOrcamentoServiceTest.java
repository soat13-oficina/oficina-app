package br.com.oficina.orcamento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.orcamento.application.command.AlterarOrcamentoCommand;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.support.persistence.TestOrcamentoRepository;

class AlterarOrcamentoServiceTest {

    @Test
    void deveAlterarOrcamentoPreservandoCriadoEmEEnvio() {
        TestOrcamentoRepository repository = new TestOrcamentoRepository();
        Orcamento orcamento = novoOrcamento();
        orcamento.enviarParaAprovacao(LocalDateTime.of(2030, 1, 2, 9, 0));
        repository.salvar(orcamento);
        AlterarOrcamentoService service = new AlterarOrcamentoService(repository);

        service.alterarOrcamento(new AlterarOrcamentoCommand(
                "orc-1",
                "os-2",
                "func-2",
                "Maria Souza",
                "99999999999",
                "XYZ9Z99",
                "Honda",
                "City",
                "Revisao de freios",
                List.of("Revisao freios"),
                List.of("Fluido de freio"),
                new BigDecimal("200.00"),
                new BigDecimal("100.00"),
                LocalDateTime.of(2031, 1, 10, 10, 0),
                "Aprovacao imediata"));

        Orcamento atualizado = repository.buscarPorId("orc-1").orElseThrow();
        assertEquals("os-2", atualizado.getOrdemDeServicoId());
        assertEquals("func-2", atualizado.getFuncionarioId());
        assertEquals("Maria Souza", atualizado.getClienteNome());
        assertEquals("99999999999", atualizado.getClienteCpf());
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
        AlterarOrcamentoService service = new AlterarOrcamentoService(new TestOrcamentoRepository());

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.alterarOrcamento(new AlterarOrcamentoCommand(
                        "orc-404",
                        "os-1",
                        "func-1",
                        "Joao Silva",
                        "12345678901",
                        "ABC1D23",
                        "Toyota",
                        "Corolla",
                        "Troca de pastilhas",
                        List.of("Troca de pastilhas"),
                        List.of("Pastilha dianteira"),
                        new BigDecimal("150.00"),
                        new BigDecimal("250.00"),
                        LocalDateTime.of(2030, 1, 10, 10, 0),
                        "Prioridade alta")));

        assertEquals("Orcamento nao encontrado para o numero informado.", exception.getMessage());
    }

    private Orcamento novoOrcamento() {
        return new Orcamento(
                "orc-1",
                "os-1",
                "func-1",
                "Joao Silva",
                "12345678901",
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Troca de pastilhas",
                List.of("Troca de pastilhas"),
                List.of("Pastilha dianteira"),
                new BigDecimal("150.00"),
                new BigDecimal("250.00"),
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 10, 10, 0),
                "Prioridade alta",
                StatusOrcamento.AGUARDANDO_APROVACAO);
    }
}
