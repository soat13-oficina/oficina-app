package br.com.oficina.ordemservico.saga.application;

import java.math.BigDecimal;
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
        public void solicitarOrcamento(UUID ordemDeServicoId, DadosOrcamento dados) {
        }

        @Override
        public void execucaoIniciada(UUID ordemDeServicoId) {
        }

        @Override
        public void execucaoConcluida(UUID ordemDeServicoId) {
        }

        @Override
        public void execucaoFalhou(UUID ordemDeServicoId, String motivo) {
        }
    };

    void iniciar(UUID ordemDeServicoId);

    /**
     * Diagnostico concluido enviado para orcamento: a Saga manda {@code GerarOrcamento} ao Billing e a
     * OS so vai para "Aguardando Aprovacao" quando o {@code OrcamentoGerado} chegar.
     *
     * @throws br.com.oficina.common.domain.exception.RegraDeNegocioException se o orcamento ja foi
     *                                                                        solicitado e a resposta nao chegou
     */
    void solicitarOrcamento(UUID ordemDeServicoId, DadosOrcamento dados);

    void execucaoIniciada(UUID ordemDeServicoId);

    void execucaoConcluida(UUID ordemDeServicoId);

    /** Mecanico abortou a execucao: a Saga estorna o pagamento e libera as pecas. */
    void execucaoFalhou(UUID ordemDeServicoId, String motivo);

    /**
     * Dados financeiros que o atendente informa no envio para orcamento. Transitorio: com o
     * diagnostico na Execution, a mao de obra vem por servico e a validade e definida pelo Billing.
     */
    record DadosOrcamento(BigDecimal valorMaoDeObra, BigDecimal desconto, LocalDateTime validade, String observacoes) {
    }
}
