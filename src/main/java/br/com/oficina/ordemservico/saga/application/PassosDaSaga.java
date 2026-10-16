package br.com.oficina.ordemservico.saga.application;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Passos da Saga que, no Passo 1 da Fase 4, ainda sao acoes HTTP do proprio OS Service: o servico
 * move a ordem e avisa a Saga por aqui. Quando diagnostico e execucao forem para o Execution
 * Service, estes avisos viram eventos ({@code DiagnosticoConcluido}, {@code ExecucaoIniciada}...).
 */
public interface PassosDaSaga {

    /** Para os testes de unidade dos casos de uso que nao exercitam a Saga. */
    PassosDaSaga NENHUM = new PassosDaSaga() {
        @Override
        public void iniciar(UUID ordemDeServicoId) {
        }

        @Override
        public UUID sagaIdDaOrdem(UUID ordemDeServicoId) {
            return null;
        }

        @Override
        public void orcamentoGerado(UUID ordemDeServicoId, UUID orcamentoId, LocalDateTime validadeOrcamento) {
        }

        @Override
        public void execucaoIniciada(UUID ordemDeServicoId) {
        }

        @Override
        public void execucaoConcluida(UUID ordemDeServicoId) {
        }
    };

    void iniciar(UUID ordemDeServicoId);

    UUID sagaIdDaOrdem(UUID ordemDeServicoId);

    void orcamentoGerado(UUID ordemDeServicoId, UUID orcamentoId, LocalDateTime validadeOrcamento);

    void execucaoIniciada(UUID ordemDeServicoId);

    void execucaoConcluida(UUID ordemDeServicoId);
}
