package br.com.oficina.support.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.com.oficina.notificacao.domain.model.Notificacao;
import br.com.oficina.notificacao.domain.model.StatusNotificacao;
import br.com.oficina.notificacao.domain.repository.NotificacaoRepository;

public class TestNotificacaoRepository implements NotificacaoRepository {
    private final Map<UUID, Notificacao> notificacoes = new ConcurrentHashMap<>();

    @Override
    public void salvar(Notificacao notificacao) {
        notificacoes.put(notificacao.getId(), notificacao);
    }

    @Override
    public Optional<Notificacao> buscarPorId(UUID id) {
        return Optional.ofNullable(notificacoes.get(id));
    }

    @Override
    public List<Notificacao> buscarReprocessaveis(int maxTentativas, LocalDateTime anteriorA) {
        return notificacoes.values().stream()
                .filter(notificacao -> notificacao.getStatus() == StatusNotificacao.PENDENTE)
                .filter(notificacao -> notificacao.getTentativas() < maxTentativas)
                .filter(notificacao -> ultimaAtividade(notificacao).isBefore(anteriorA))
                .toList();
    }

    @Override
    public List<Notificacao> buscarTodas() {
        return List.copyOf(notificacoes.values());
    }

    private LocalDateTime ultimaAtividade(Notificacao notificacao) {
        return notificacao.getUltimaTentativaEm() != null
                ? notificacao.getUltimaTentativaEm()
                : notificacao.getCriadoEm();
    }
}
