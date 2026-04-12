package br.com.oficina.orcamento.infrastructure.web.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.oficina.orcamento.domain.model.Orcamento;

class OrcamentoResponseTest {

    @Test
    void deveConverterOrcamentoParaResponse() {
        Orcamento orcamento = new Orcamento(
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

        OrcamentoResponse response = OrcamentoResponse.from(orcamento);

        assertEquals("orc-1", response.id());
        assertEquals("os-1", response.ordemDeServicoId());
        assertEquals("func-1", response.funcionarioId());
        assertEquals("cliente-1", response.clienteId());
        assertEquals("ABC1D23", response.placaVeiculo());
        assertEquals(new BigDecimal("400.00"), response.valorTotal());
        assertEquals("Prioridade alta", response.observacoes());
    }
}
