package br.com.oficina.orcamento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.oficina.orcamento.application.command.ExcluirOrcamentoCommand;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.infrastructure.persistence.InMemoryOrcamentoRepository;

class ExcluirOrcamentoServiceTest {

    @Test
    void deveExcluirOrcamentoExistente() {
        InMemoryOrcamentoRepository repository = new InMemoryOrcamentoRepository();
        repository.salvar(novoOrcamento());
        ExcluirOrcamentoService service = new ExcluirOrcamentoService(repository);

        service.excluirOrcamento(new ExcluirOrcamentoCommand("orc-1"));

        assertTrue(repository.buscarPorId("orc-1").isEmpty());
    }

    @Test
    void deveFalharAoExcluirOrcamentoInexistente() {
        ExcluirOrcamentoService service = new ExcluirOrcamentoService(new InMemoryOrcamentoRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.excluirOrcamento(new ExcluirOrcamentoCommand("orc-404")));

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
