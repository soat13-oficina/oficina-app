package br.com.oficina.orcamento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.oficina.orcamento.application.command.AlterarOrcamentoCommand;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.infrastructure.persistence.InMemoryOrcamentoRepository;

class AlterarOrcamentoServiceTest {

    @Test
    void deveAlterarOrcamentoPreservandoCriadoEmEEnvio() {
        InMemoryOrcamentoRepository repository = new InMemoryOrcamentoRepository();
        Orcamento orcamento = novoOrcamento();
        orcamento.enviarParaAprovacao(LocalDateTime.of(2030, 1, 2, 9, 0));
        repository.salvar(orcamento);
        AlterarOrcamentoService service = new AlterarOrcamentoService(repository);

        service.alterarOrcamento(new AlterarOrcamentoCommand(
                "orc-1",
                "os-2",
                "func-2",
                "cliente-2",
                "XYZ9Z99",
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
        assertEquals("cliente-2", atualizado.getClienteId());
        assertEquals("XYZ9Z99", atualizado.getPlacaVeiculo());
        assertEquals(new BigDecimal("300.00"), atualizado.getValorTotal());
        assertEquals(LocalDateTime.of(2030, 1, 1, 10, 0), atualizado.getCriadoEm());
        assertEquals(LocalDateTime.of(2030, 1, 2, 9, 0), atualizado.getEnviadoParaAprovacaoEm());
    }

    @Test
    void deveFalharAoAlterarOrcamentoInexistente() {
        AlterarOrcamentoService service = new AlterarOrcamentoService(new InMemoryOrcamentoRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarOrcamento(new AlterarOrcamentoCommand(
                        "orc-404",
                        "os-1",
                        "func-1",
                        "cliente-1",
                        "ABC1D23",
                        "Troca de pastilhas",
                        List.of("Troca de pastilhas"),
                        List.of("Pastilha dianteira"),
                        new BigDecimal("150.00"),
                        new BigDecimal("250.00"),
                        LocalDateTime.of(2030, 1, 10, 10, 0),
                        "Prioridade alta")));

        assertEquals("Orcamento nao encontrado", exception.getMessage());
    }

    private Orcamento novoOrcamento() {
        return new Orcamento(
                "orc-1",
                "os-1",
                "func-1",
                "cliente-1",
                "ABC1D23",
                "Troca de pastilhas",
                List.of("Troca de pastilhas"),
                List.of("Pastilha dianteira"),
                new BigDecimal("150.00"),
                new BigDecimal("250.00"),
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 10, 10, 0),
                "Prioridade alta");
    }
}
