package br.com.oficina.ordemservico.infrastructure.web.response;

import br.com.oficina.ordemservico.application.usecase.ConsultarTempoMedioExecucaoUseCase.TempoMedioExecucao;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TempoMedioExecucaoResponse", description = "Métrica de tempo médio de execução das ordens de serviço finalizadas")
public record TempoMedioExecucaoResponse(
        long tempoMedioExecucaoEmSegundos,
        String tempoMedioExecucaoFormatado,
        long quantidadeOrdensConsideradas) {
    @Override
    @Schema(description = "Tempo médio geral em segundos", example = "7200")
    public long tempoMedioExecucaoEmSegundos() {
        return tempoMedioExecucaoEmSegundos;
    }

    @Override
    @Schema(description = "Tempo médio geral formatado para leitura humana", example = "2 horas")
    public String tempoMedioExecucaoFormatado() {
        return tempoMedioExecucaoFormatado;
    }

    @Override
    @Schema(description = "Quantidade de ordens consideradas no cálculo", example = "5")
    public long quantidadeOrdensConsideradas() {
        return quantidadeOrdensConsideradas;
    }

    public static TempoMedioExecucaoResponse from(TempoMedioExecucao tempoMedioExecucao) {
        return new TempoMedioExecucaoResponse(
                tempoMedioExecucao.tempoMedioExecucaoEmSegundos(),
                tempoMedioExecucao.tempoMedioExecucaoFormatado(),
                tempoMedioExecucao.quantidadeOrdensConsideradas());
    }
}
