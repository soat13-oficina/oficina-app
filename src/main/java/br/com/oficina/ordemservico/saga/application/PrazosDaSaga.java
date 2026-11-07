package br.com.oficina.ordemservico.saga.application;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Prazos da Saga (ADR 0004, tabela "Timeouts"), em ISO-8601. Curtos em hml para a demonstracao.
 */
@Component
public class PrazosDaSaga {

    private final Duration decisao;
    private final Duration decisaoPadrao;
    private final Duration resposta;
    private final int maxTentativas;
    private final Duration pagamento;
    private final Duration margemPagamento;

    /**
     * @param decisao         decisao do cliente; nulo = vale a validade do orcamento (producao)
     * @param decisaoPadrao   decisao do cliente quando o orcamento nao tem validade
     * @param resposta        resposta imediata de um participante a um comando
     * @param maxTentativas   reenvios de um comando antes de tratar o passo como falha
     * @param pagamento       prazo dado ao cliente para pagar (vai no SolicitarPagamento)
     * @param margemPagamento rede de seguranca: o Billing e quem expira o pagamento; o OS so age se
     *                        nada chegar ate o prazo + margem
     */
    public PrazosDaSaga(
            @Value("${saga.prazo-decisao:#{null}}") Duration decisao,
            @Value("${saga.prazo-decisao-padrao:P7D}") Duration decisaoPadrao,
            @Value("${saga.prazo-resposta:PT5M}") Duration resposta,
            @Value("${saga.max-tentativas:3}") int maxTentativas,
            @Value("${saga.prazo-pagamento:PT24H}") Duration pagamento,
            @Value("${saga.margem-pagamento:PT10M}") Duration margemPagamento) {
        this.decisao = decisao;
        this.decisaoPadrao = decisaoPadrao;
        this.resposta = resposta;
        this.maxTentativas = maxTentativas;
        this.pagamento = pagamento;
        this.margemPagamento = margemPagamento;
    }

    /**
     * {@code saga.prazo-decisao} (curto em hml) tem prioridade; senao vale a validade do orcamento;
     * sem validade futura (ou decisao reaberta depois de uma aprovacao revertida), o prazo padrao.
     */
    public LocalDateTime decisao(LocalDateTime validadeOrcamento, LocalDateTime agora) {
        if (decisao != null) {
            return agora.plus(decisao);
        }
        return validadeOrcamento != null && validadeOrcamento.isAfter(agora) ? validadeOrcamento : agora.plus(decisaoPadrao);
    }

    public Duration resposta() {
        return resposta;
    }

    public int maxTentativas() {
        return maxTentativas;
    }

    public Duration pagamento() {
        return pagamento;
    }

    public Duration margemPagamento() {
        return margemPagamento;
    }
}
