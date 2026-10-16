package br.com.oficina.ordemservico.saga.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.ordemservico.saga.domain.model.RegistroPassoSaga;
import br.com.oficina.ordemservico.saga.domain.model.SagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.model.StatusSaga;
import br.com.oficina.ordemservico.saga.domain.repository.SagaOrdemServicoRepository;

@Repository
@Transactional(readOnly = true)
public class JpaSagaOrdemServicoRepository implements SagaOrdemServicoRepository {

    private static final List<StatusSaga> FINAIS = Arrays.stream(StatusSaga.values()).filter(StatusSaga::isFinal).toList();

    private final SpringDataSagaOrdemServicoRepository sagas;
    private final SpringDataRegistroPassoSagaRepository passos;

    public JpaSagaOrdemServicoRepository(SpringDataSagaOrdemServicoRepository sagas, SpringDataRegistroPassoSagaRepository passos) {
        this.sagas = sagas;
        this.passos = passos;
    }

    @Override
    @Transactional
    public void salvar(SagaOrdemServico saga) {
        sagas.save(saga);
    }

    @Override
    public Optional<SagaOrdemServico> buscarPorOrdemDeServico(UUID ordemDeServicoId) {
        return sagas.findByOrdemDeServicoId(ordemDeServicoId);
    }

    @Override
    public List<UUID> buscarIdsComPrazoVencido(LocalDateTime agora) {
        return sagas.buscarIdsComPrazoVencido(agora, FINAIS);
    }

    @Override
    public Optional<SagaOrdemServico> buscarPorId(UUID sagaId) {
        return sagas.findById(sagaId);
    }

    @Override
    @Transactional
    public void registrarPasso(RegistroPassoSaga registro) {
        passos.save(registro);
    }

    @Override
    public List<RegistroPassoSaga> listarPassos(UUID sagaId) {
        return passos.findBySagaIdOrderByOcorridoEmAscIdAsc(sagaId);
    }
}
