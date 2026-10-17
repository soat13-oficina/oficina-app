# language: pt
Funcionalidade: Reserva de peças, pagamento e compensações da Saga
  Como oficina, quero que as peças fiquem reservadas só para ordens aprovadas e pagas e que, quando
  um passo falha, a Saga desfaça o que os passos anteriores fizeram (ADR 0004).

  Contexto:
    Dado a peça "Filtro de óleo" com 5 unidades em estoque
    E uma ordem de serviço aberta para o veículo "BDD2B34"

  Cenário: Ordem paga tem as peças reservadas e a conclusão baixa o estoque
    Dado o diagnóstico concluído pelo mecânico com o serviço "Troca do filtro" e 2 unidades de "Filtro de óleo"
    E o orçamento gerado pela saga
    Quando o cliente aprova o orçamento
    Então a ordem fica "Aguardando Pagamento"
    E a peça "Filtro de óleo" fica com 2 unidades reservadas e 5 em estoque
    Quando o cliente paga
    E o mecânico inicia e conclui a execução
    Então a ordem fica "Finalizada" com motivo "SERVICO_CONCLUIDO"
    E a peça "Filtro de óleo" fica com 0 unidades reservadas e 3 em estoque

  Cenário: Estoque insuficiente reverte a aprovação e o cliente decide de novo
    Dado que restou só 1 unidade de "Filtro de óleo"
    E o diagnóstico concluído pelo mecânico com o serviço "Troca do filtro" e 2 unidades de "Filtro de óleo"
    E o orçamento gerado pela saga
    Quando o cliente aprova o orçamento
    Então a ordem fica "Aguardando Aprovação"
    E a saga fica "AGUARDANDO_DECISAO"
    E o orçamento fica "AGUARDANDO_APROVACAO"
    E a peça "Filtro de óleo" fica com 0 unidades reservadas e 1 em estoque
    E a história da saga registra o comando "ReverterAprovacao" e a confirmação "AprovacaoRevertida"

  Cenário: Pagamento recusado cancela a ordem, libera as peças e cancela a execução
    Dado o diagnóstico concluído pelo mecânico com o serviço "Troca do filtro" e 2 unidades de "Filtro de óleo"
    E o orçamento gerado pela saga
    E o cliente aprova o orçamento
    Quando o pagamento é recusado
    Então a ordem fica "Cancelada" com motivo "PAGAMENTO_RECUSADO"
    E o orçamento fica "CANCELADO"
    E o pagamento fica "RECUSADO"
    E a execução fica "CANCELADA" na Execution
    E a peça "Filtro de óleo" fica com 0 unidades reservadas e 5 em estoque
    E a saga termina como "COMPENSADA"

  Cenário: Pagamento não feito no prazo expira e cancela a ordem
    Dado o diagnóstico concluído pelo mecânico com o serviço "Troca do filtro" e 2 unidades de "Filtro de óleo"
    E o orçamento gerado pela saga
    E o cliente aprova o orçamento
    Quando o prazo de pagamento vence
    Então a ordem fica "Cancelada" com motivo "PAGAMENTO_EXPIRADO"
    E o orçamento fica "CANCELADO"
    E o pagamento fica "EXPIRADO"
    E a peça "Filtro de óleo" fica com 0 unidades reservadas e 5 em estoque
    E a saga termina como "COMPENSADA"

  Cenário: Execução abortada estorna o pagamento e libera as peças
    Dado o diagnóstico concluído pelo mecânico com o serviço "Troca do filtro" e 2 unidades de "Filtro de óleo"
    E o orçamento gerado pela saga
    E o cliente aprova o orçamento
    E o cliente paga
    Quando o mecânico inicia a execução e aborta com o motivo "Peça incompatível com o motor"
    Então a ordem fica "Cancelada" com motivo "FALHA_EXECUCAO"
    E o pagamento fica "ESTORNADO"
    E a execução fica "ABORTADA" na Execution
    E a peça "Filtro de óleo" fica com 0 unidades reservadas e 5 em estoque
    E a saga termina como "COMPENSADA"
    E a história da saga registra o comando "EstornarPagamento" e a confirmação "PagamentoEstornado"

  Cenário: Peça que saiu do catálogo devolve o diagnóstico ao mecânico
    Dado que a peça "Filtro de óleo" saiu do catálogo
    Quando o mecânico inicia e conclui o diagnóstico com o serviço "Troca do filtro" e 2 unidades de "Filtro de óleo"
    Então a ordem fica "Diagnóstico"
    E o diagnóstico volta ao mecânico sem as peças
    E a execução fica "AGUARDANDO_DIAGNOSTICO" na Execution
    E a saga fica "AGUARDANDO_DIAGNOSTICO"
    E nenhum orçamento é gerado
