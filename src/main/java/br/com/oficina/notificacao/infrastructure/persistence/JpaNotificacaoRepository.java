package br.com.oficina.notificacao.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.notificacao.domain.model.Notificacao;
import br.com.oficina.notificacao.domain.repository.NotificacaoRepository;

@Repository
@Transactional(readOnly = true)
public class JpaNotificacaoRepository implements NotificacaoRepository {
    private final SpringDataNotificacaoRepository repository;

    public JpaNotificacaoRepository(SpringDataNotificacaoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void salvar(Notificacao notificacao) {
        repository.save(NotificacaoJpaEntity.fromDomain(notificacao));
    }

    @Override
    public Optional<Notificacao> buscarPorId(UUID id) {
        return repository.findById(id).map(NotificacaoJpaEntity::toDomain);
    }

    @Override
    public List<Notificacao> buscarReprocessaveis(int maxTentativas, LocalDateTime anteriorA) {
        return repository.buscarReprocessaveis(maxTentativas, anteriorA).stream()
                .map(NotificacaoJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Notificacao> buscarTodas() {
        return repository.findAll().stream().map(NotificacaoJpaEntity::toDomain).toList();
    }
}
