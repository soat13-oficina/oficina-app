package br.com.oficina.ordemservico.domain.model;

/**
 * Situação de negócio (rótulo canônico) exposta pelas APIs, conforme o desafio da Fase 2.
 * Mapeia os estados internos mais granulares de {@link StatusOrdemDeServico} para as seis
 * situações de negócio: Recebida, Diagnóstico, Aguardando Aprovação, Execução, Finalizada, Entregue.
 */
public enum SituacaoOrdemDeServico {
    RECEBIDA("Recebida"),
    DIAGNOSTICO("Diagnóstico"),
    AGUARDANDO_APROVACAO("Aguardando Aprovação"),
    EXECUCAO("Execução"),
    FINALIZADA("Finalizada"),
    ENTREGUE("Entregue");

    private final String descricao;

    SituacaoOrdemDeServico(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static SituacaoOrdemDeServico fromStatus(StatusOrdemDeServico status) {
        return switch (status) {
            case OS_ABERTA -> RECEBIDA;
            case DIAGNOSTICO_EM_ANDAMENTO, DIAGNOSTICO_CONCLUIDO -> DIAGNOSTICO;
            case ORCAMENTO_GERADO, AGUARDANDO_APROVACAO -> AGUARDANDO_APROVACAO;
            case SERVICO_EM_ANDAMENTO -> EXECUCAO;
            case OS_FINALIZADA -> FINALIZADA;
            case ENTREGUE -> ENTREGUE;
        };
    }
}
