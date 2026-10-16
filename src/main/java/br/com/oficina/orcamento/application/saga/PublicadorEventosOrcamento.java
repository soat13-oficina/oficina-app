package br.com.oficina.orcamento.application.saga;

import java.time.Instant;

import org.springframework.stereotype.Component;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;
import br.com.oficina.orcamento.domain.model.CanalDecisao;
import br.com.oficina.orcamento.domain.model.Orcamento;

@Component
public class PublicadorEventosOrcamento implements EventosOrcamento {

    private final PublicadorMensagens publicador;
    private final FabricaEnvelopes fabrica;

    public PublicadorEventosOrcamento(PublicadorMensagens publicador, FabricaEnvelopes fabrica) {
        this.publicador = publicador;
        this.fabrica = fabrica;
    }

    @Override
    public void orcamentoAprovado(Orcamento orcamento, CanalDecisao canal) {
        decidido(MensagensOrcamento.ORCAMENTO_APROVADO, orcamento, canal);
    }

    @Override
    public void orcamentoRejeitado(Orcamento orcamento, CanalDecisao canal) {
        decidido(MensagensOrcamento.ORCAMENTO_REJEITADO, orcamento, canal);
    }

    @Override
    public void orcamentoCancelado(Envelope comando, Orcamento orcamento) {
        publicador.publicar(Filas.OS_SAGA_REPLIES, fabrica.resposta(comando, MensagensOrcamento.ORCAMENTO_CANCELADO,
                new MensagensOrcamento.OrcamentoCancelado(orcamento.getId(), false)));
    }

    // Decisao e evento espontaneo (acao do cliente), nao resposta a comando: causationId nulo.
    private void decidido(String tipo, Orcamento orcamento, CanalDecisao canal) {
        publicador.publicar(Filas.OS_SAGA_REPLIES, fabrica.novo(tipo, orcamento.getSagaId(), orcamento.getOrdemDeServicoId(),
                new MensagensOrcamento.OrcamentoDecidido(orcamento.getId(), canal.name(), Instant.now())));
    }
}
