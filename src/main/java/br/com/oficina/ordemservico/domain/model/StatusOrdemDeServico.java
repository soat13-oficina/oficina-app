package br.com.oficina.ordemservico.domain.model;

public enum StatusOrdemDeServico {
    OS_ABERTA,
    DIAGNOSTICO_EM_ANDAMENTO,
    DIAGNOSTICO_CONCLUIDO,
    ORCAMENTO_GERADO,
    AGUARDANDO_APROVACAO,
    SERVICO_EM_ANDAMENTO,
    OS_FINALIZADA,
    ENTREGUE
}
