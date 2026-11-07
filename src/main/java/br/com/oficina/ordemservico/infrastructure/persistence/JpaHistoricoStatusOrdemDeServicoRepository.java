package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.ordemservico.domain.model.AlteracaoStatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.HistoricoStatusOrdemDeServicoRepository;

@Repository
@Transactional(readOnly = true)
public class JpaHistoricoStatusOrdemDeServicoRepository implements HistoricoStatusOrdemDeServicoRepository {

    private final SpringDataHistoricoStatusOrdemDeServicoRepository repository;

    public JpaHistoricoStatusOrdemDeServicoRepository(SpringDataHistoricoStatusOrdemDeServicoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void registrar(UUID ordemDeServicoId, List<AlteracaoStatusOrdemDeServico> alteracoes) {
        if (alteracoes.isEmpty()) {
            return;
        }
        repository.saveAll(alteracoes.stream()
                .map(alteracao -> HistoricoStatusOrdemDeServicoEntity.de(ordemDeServicoId, alteracao))
                .toList());
    }

    @Override
    public List<AlteracaoStatusOrdemDeServico> listarPorOrdem(UUID ordemDeServicoId) {
        return repository.findByOrdemDeServicoIdOrderByOcorridoEmAscIdAsc(ordemDeServicoId).stream()
                .map(HistoricoStatusOrdemDeServicoEntity::paraDominio)
                .toList();
    }
}
