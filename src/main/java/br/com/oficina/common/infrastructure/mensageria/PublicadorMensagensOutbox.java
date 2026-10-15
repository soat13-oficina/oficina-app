package br.com.oficina.common.infrastructure.mensageria;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;
import tools.jackson.databind.json.JsonMapper;

/**
 * Implementa a porta gravando no outbox. {@link Propagation#MANDATORY}: publicar fora de uma
 * transacao de negocio quebraria a garantia do padrao, entao e erro de programacao e falha alto.
 */
@Component
public class PublicadorMensagensOutbox implements PublicadorMensagens {

    private final SpringDataOutboxMensagemRepository repository;
    private final JsonMapper jsonMapper;

    public PublicadorMensagensOutbox(SpringDataOutboxMensagemRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publicar(String fila, Envelope envelope) {
        repository.save(new OutboxMensagemEntity(
                envelope.messageId(),
                fila,
                envelope.type(),
                envelope.ordemServicoId().toString(),
                jsonMapper.writeValueAsString(envelope),
                LocalDateTime.now()));
    }
}
