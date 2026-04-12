package br.com.oficina.orcamento.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class OrcamentoTest {

    @Test
    void deveExporDadosDoOrcamentoECalcularValorTotal() {
        LocalDateTime criadoEm = LocalDateTime.of(2030, 1, 1, 10, 0);
        LocalDateTime validade = LocalDateTime.of(2030, 1, 10, 10, 0);
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
                criadoEm,
                validade,
                "Prioridade alta");

        orcamento.enviarParaAprovacao(LocalDateTime.of(2030, 1, 2, 9, 0));

        assertEquals("orc-1", orcamento.getId());
        assertEquals("os-1", orcamento.getOrdemDeServicoId());
        assertEquals("func-1", orcamento.getFuncionarioId());
        assertEquals("cliente-1", orcamento.getClienteId());
        assertEquals("ABC1D23", orcamento.getPlacaVeiculo());
        assertEquals("Troca de pastilhas", orcamento.getDescricaoDiagnostico());
        assertEquals(List.of("Troca de pastilhas"), orcamento.getServicosPropostos());
        assertEquals(List.of("Pastilha dianteira"), orcamento.getPecasPrevistas());
        assertEquals(new BigDecimal("150.00"), orcamento.getValorMaoDeObra());
        assertEquals(new BigDecimal("250.00"), orcamento.getValorPecas());
        assertEquals(new BigDecimal("400.00"), orcamento.getValorTotal());
        assertEquals(criadoEm, orcamento.getCriadoEm());
        assertEquals(validade, orcamento.getValidade());
        assertEquals("Prioridade alta", orcamento.getObservacoes());
        assertEquals(LocalDateTime.of(2030, 1, 2, 9, 0), orcamento.getEnviadoParaAprovacaoEm());
    }
}
