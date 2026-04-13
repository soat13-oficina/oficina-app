package br.com.oficina.orcamento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.oficina.orcamento.application.command.CadastrarNovoOrcamentoCommand;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.support.persistence.TestOrcamentoRepository;

class CadastrarNovoOrcamentoServiceTest {

    @Test
    void deveCadastrarNovoOrcamento() {
        TestOrcamentoRepository repository = new TestOrcamentoRepository();
        CadastrarNovoOrcamentoService service = new CadastrarNovoOrcamentoService(repository);

        service.cadastrarNovoOrcamento(new CadastrarNovoOrcamentoCommand(
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
                LocalDateTime.of(2030, 1, 10, 10, 0),
                "Prioridade alta"));

        assertEquals(1, repository.buscarTodos().size());
        assertEquals(new BigDecimal("400.00"), repository.buscarPorNumeroOrcamento("orc-1").orElseThrow().getValorTotal());
        assertEquals(StatusOrcamento.AGUARDANDO_APROVACAO, repository.buscarPorNumeroOrcamento("orc-1").orElseThrow().getStatus());
        assertNotNull(repository.buscarPorNumeroOrcamento("orc-1").orElseThrow().getId());
    }
}
