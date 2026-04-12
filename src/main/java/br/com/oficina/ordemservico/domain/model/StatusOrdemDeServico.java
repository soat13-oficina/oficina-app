package br.com.oficina.ordemservico.domain.model;

public enum StatusOrdemDeServico {
    ABERTA,
    DIAGNOSTICO_EM_ANDAMENTO,
    DIAGNOSTICO_CONCLUIDO,
    AGUARDANDO_ORCAMENTO,
    ORCAMENTO_GERADO,
    AGUARDANDO_APROVACAO
}
