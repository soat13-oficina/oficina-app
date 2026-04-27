package br.com.oficina.ordemservico.infrastructure.web.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.oficina.ordemservico.application.usecase.ConsultarTempoMedioExecucaoUseCase.TempoMedioExecucao;

class TempoMedioExecucaoResponseTest {

    @Test
    void deveConverterTempoMedioExecucaoParaResponse() {
        TempoMedioExecucaoResponse response = TempoMedioExecucaoResponse.from(
                new TempoMedioExecucao(7200, "2 horas", 4));

        assertEquals(7200, response.tempoMedioExecucaoEmSegundos());
        assertEquals("2 horas", response.tempoMedioExecucaoFormatado());
        assertEquals(4, response.quantidadeOrdensConsideradas());
    }
}
