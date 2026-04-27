package br.com.oficina.ordemservico.application.service;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.ordemservico.application.usecase.ConsultarTempoMedioExecucaoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class ConsultarTempoMedioExecucaoService implements ConsultarTempoMedioExecucaoUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConsultarTempoMedioExecucaoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public ConsultarTempoMedioExecucaoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public TempoMedioExecucao consultarTempoMedioExecucao() {
        log.info("Consultando tempo medio de execucao das ordens de servico finalizadas.");
        List<OrdemDeServico> ordensFinalizadas = ordemDeServicoRepository.buscarOrdensComExecucaoFinalizada();
        if (ordensFinalizadas.isEmpty()) {
            log.info("Nenhuma ordem com execucao finalizada encontrada para calculo da metrica.");
            return new TempoMedioExecucao(0, "0 minutos", 0);
        }

        long mediaSegundos = Math.round(ordensFinalizadas.stream()
                .mapToLong(ordem -> Duration.between(ordem.getIniciadaEm(), ordem.getFinalizadaEm()).getSeconds())
                .average()
                .orElse(0));

        log.info("Tempo medio de execucao calculado. quantidadeOrdensConsideradas={}, tempoMedioExecucaoEmSegundos={}",
                ordensFinalizadas.size(),
                mediaSegundos);
        return new TempoMedioExecucao(
                mediaSegundos,
                formatarDuracao(Duration.ofSeconds(mediaSegundos)),
                ordensFinalizadas.size());
    }

    private String formatarDuracao(Duration duracao) {
        long totalMinutos = duracao.toMinutes();
        if (totalMinutos < 60) {
            return totalMinutos + " minuto" + (totalMinutos == 1 ? "" : "s");
        }

        long horas = totalMinutos / 60;
        long minutosRestantes = totalMinutos % 60;
        if (minutosRestantes == 0) {
            return horas + " hora" + (horas == 1 ? "" : "s");
        }

        return horas + " hora" + (horas == 1 ? "" : "s") + " e "
                + minutosRestantes + " minuto" + (minutosRestantes == 1 ? "" : "s");
    }
}
