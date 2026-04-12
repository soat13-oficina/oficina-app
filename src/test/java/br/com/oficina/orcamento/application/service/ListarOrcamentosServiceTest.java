package br.com.oficina.orcamento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.oficina.orcamento.application.query.ListarOrcamentosQuery;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.support.persistence.TestOrcamentoRepository;

class ListarOrcamentosServiceTest {

    @Test
    void deveListarTodosOsOrcamentos() {
        TestOrcamentoRepository repository = new TestOrcamentoRepository();
        repository.salvar(novoOrcamento("orc-1"));
        repository.salvar(novoOrcamento("orc-2"));
        ListarOrcamentosService service = new ListarOrcamentosService(repository);

        List<Orcamento> resultado = service.listarOrcamentos(new ListarOrcamentosQuery());

        assertEquals(2, resultado.size());
    }

    private Orcamento novoOrcamento(String id) {
        return new Orcamento(
                id,
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
