package br.com.oficina.domain.model.ordemservico;

public enum StatusOrdemDeServico {
    ABERTA,
    DIAGNOSTICO_EM_ANDAMENTO,
    DIAGNOSTICO_CONCLUIDO,
    AGUARDANDO_ORCAMENTO,
    ORCAMENTO_GERADO,
    AGUARDANDO_APROVACAO
}
