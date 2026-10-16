package br.com.oficina.ordemservico.saga.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.oficina.ordemservico.saga.domain.model.RegistroPassoSaga;
import br.com.oficina.ordemservico.saga.domain.model.SagaOrdemServico;

public interface SagaOrdemServicoRepository {

    void salvar(SagaOrdemServico saga);

    Optional<SagaOrdemServico> buscarPorOrdemDeServico(UUID ordemDeServicoId);

    /** Ids das Sagas nao finalizadas com prazo vencido, mais antigas primeiro. */
    List<UUID> buscarIdsComPrazoVencido(LocalDateTime agora);

    Optional<SagaOrdemServico> buscarPorId(UUID sagaId);

    void registrarPasso(RegistroPassoSaga registro);

    List<RegistroPassoSaga> listarPassos(UUID sagaId);
}
