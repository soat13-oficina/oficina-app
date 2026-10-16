package br.com.oficina.orcamento.application.saga;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.orcamento.domain.model.CanalDecisao;
import br.com.oficina.orcamento.domain.model.Orcamento;

/**
 * Eventos que o orcamento publica para o orquestrador da Saga ({@code os-saga-replies}). Gravados
 * no outbox na transacao corrente: so saem se a decisao confirmar.
 */
public interface EventosOrcamento {

    /** Para os testes de unidade dos casos de uso que nao exercitam a mensageria. */
    EventosOrcamento NENHUM = new EventosOrcamento() {
        @Override
        public void orcamentoAprovado(Orcamento orcamento, CanalDecisao canal) {
        }

        @Override
        public void orcamentoRejeitado(Orcamento orcamento, CanalDecisao canal) {
        }

        @Override
        public void orcamentoCancelado(Envelope comando, Orcamento orcamento) {
        }
    };

    void orcamentoAprovado(Orcamento orcamento, CanalDecisao canal);

    void orcamentoRejeitado(Orcamento orcamento, CanalDecisao canal);

    /** Confirmacao da compensacao {@code CancelarOrcamento}, com {@code causationId} = o comando. */
    void orcamentoCancelado(Envelope comando, Orcamento orcamento);
}
