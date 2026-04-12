package br.com.oficina.orcamento.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.oficina.orcamento.domain.model.Orcamento;

class InMemoryOrcamentoRepositoryTest {

    @Test
    void deveSalvarBuscarListarAtualizarEExcluirOrcamento() {
        InMemoryOrcamentoRepository repository = new InMemoryOrcamentoRepository();
        Orcamento original = novoOrcamento("orc-1", "Troca de pastilhas", "400.00");

        repository.salvar(original);
        assertEquals("Troca de pastilhas", repository.buscarPorId("orc-1").orElseThrow().getDescricaoDiagnostico());
        assertEquals(1, repository.buscarTodos().size());

        repository.atualizar(novoOrcamento("orc-1", "Revisao de freios", "300.00"));
        assertEquals("Revisao de freios", repository.buscarPorId("orc-1").orElseThrow().getDescricaoDiagnostico());

        repository.excluirPorId("orc-1");
        assertTrue(repository.buscarPorId("orc-1").isEmpty());
    }

    private Orcamento novoOrcamento(String id, String descricao, String total) {
        BigDecimal valorMaoDeObra = new BigDecimal("100.00");
        BigDecimal valorPecas = new BigDecimal(total).subtract(valorMaoDeObra);
        return new Orcamento(
                id,
                "os-1",
                "func-1",
                "cliente-1",
                "ABC1D23",
                descricao,
                List.of("Servico"),
                List.of("Peca"),
                valorMaoDeObra,
                valorPecas,
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 10, 10, 0),
                "Observacao");
    }
}
