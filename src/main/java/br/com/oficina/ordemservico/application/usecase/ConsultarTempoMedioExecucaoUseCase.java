package br.com.oficina.ordemservico.application.usecase;

public interface ConsultarTempoMedioExecucaoUseCase {
    TempoMedioExecucao consultarTempoMedioExecucao();

    record TempoMedioExecucao(
            long tempoMedioExecucaoEmSegundos,
            String tempoMedioExecucaoFormatado,
            long quantidadeOrdensConsideradas) {
    }
}
