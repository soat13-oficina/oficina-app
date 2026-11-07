package br.com.oficina.ordemservico.saga.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.saga.domain.model.RegistroPassoSaga;
import br.com.oficina.ordemservico.saga.domain.model.SagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.repository.SagaOrdemServicoRepository;

/** Estado e historia de uma Saga, para {@code GET /sagas/{ordemServicoId}}. */
@Service
@Transactional(readOnly = true)
public class ConsultarSagaUseCase {

    private final SagaOrdemServicoRepository sagas;

    public ConsultarSagaUseCase(SagaOrdemServicoRepository sagas) {
        this.sagas = sagas;
    }

    public SagaConsultada consultar(UUID ordemDeServicoId) {
        SagaOrdemServico saga = sagas.buscarPorOrdemDeServico(ordemDeServicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Saga nao encontrada para a ordem de servico informada."));
        return new SagaConsultada(saga, sagas.listarPassos(saga.getId()));
    }

    public record SagaConsultada(SagaOrdemServico saga, List<RegistroPassoSaga> passos) {
    }
}
