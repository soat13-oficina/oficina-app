package br.com.oficina.orcamento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.oficina.orcamento.application.query.ConsultarOrcamentoQuery;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.infrastructure.persistence.InMemoryOrcamentoRepository;

class ConsultarOrcamentoServiceTest {

    @Test
    void deveConsultarOrcamentoPorId() {
        InMemoryOrcamentoRepository repository = new InMemoryOrcamentoRepository();
        repository.salvar(novoOrcamento());
        ConsultarOrcamentoService service = new ConsultarOrcamentoService(repository);

        Orcamento orcamento = service.consultarOrcamento(new ConsultarOrcamentoQuery("orc-1")).orElseThrow();

        assertEquals("orc-1", orcamento.getId());
    }

    @Test
    void deveRetornarVazioQuandoOrcamentoNaoExistir() {
        ConsultarOrcamentoService service = new ConsultarOrcamentoService(new InMemoryOrcamentoRepository());

        assertTrue(service.consultarOrcamento(new ConsultarOrcamentoQuery("orc-404")).isEmpty());
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
