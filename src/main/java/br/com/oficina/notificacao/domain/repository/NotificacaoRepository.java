package br.com.oficina.notificacao.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.oficina.notificacao.domain.model.Notificacao;

public interface NotificacaoRepository {
    void salvar(Notificacao notificacao);

    Optional<Notificacao> buscarPorId(UUID id);

    /**
     * Notificações elegíveis a reprocessamento: {@code PENDENTE} com {@code tentativas < maxTentativas} e
     * cuja última atividade ({@code COALESCE(ultimaTentativaEm, criadoEm)}) seja anterior a {@code anteriorA}
     * (período de carência — evita corrida com o envio ativo do listener).
     */
    List<Notificacao> buscarReprocessaveis(int maxTentativas, LocalDateTime anteriorA);

    List<Notificacao> buscarTodas();
}
