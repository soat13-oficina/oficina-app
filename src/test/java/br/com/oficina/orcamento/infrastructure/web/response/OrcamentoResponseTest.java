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
                br.com.oficina.orcamento.domain.model.StatusOrcamento.AGUARDANDO_APROVACAO);

        OrcamentoResponse response = OrcamentoResponse.from(orcamento);

        assertEquals("orc-1", response.numeroOrcamento());
        assertEquals("Joao Silva", response.cliente().nome());
        assertEquals("12345678901", response.cliente().cpf());
        assertEquals("ABC1D23", response.veiculo().placa());
        assertEquals("Toyota", response.veiculo().marca());
        assertEquals(new BigDecimal("400.00"), response.detalhesServico().valorTotal());
        assertEquals("Prioridade alta", response.detalhesServico().observacoes());
        assertEquals("AGUARDANDO_APROVACAO", response.status());
    }
}
