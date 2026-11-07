# language: pt
Funcionalidade: Ciclo de vida da ordem de serviço coordenado pela Saga
  Como oficina, quero que a ordem de serviço avance conforme a decisão do cliente sobre o
  orçamento e o pagamento, sem que o orçamento altere a ordem diretamente: o orçamento publica a
  decisão e o orquestrador da Saga move a ordem (ADR 0004).

  Contexto:
    Dado uma ordem de serviço aberta para o veículo "BDD1A23"
    E o diagnóstico concluído pelo mecânico com o serviço "Troca de óleo e filtro"
    E o orçamento gerado pela saga

  Cenário: Cliente aprova, paga e a ordem segue até a finalização
    Então a ordem fica "Aguardando Aprovação"
    E a saga fica "AGUARDANDO_DECISAO"
    Quando o cliente aprova o orçamento
    Então a ordem fica "Aguardando Pagamento"
    E a saga fica "AGUARDANDO_PAGAMENTO"
    E o cliente recebe o link de pagamento
    Quando o cliente paga
    Então a ordem fica "Execução"
    E a saga fica "AGUARDANDO_EXECUCAO"
    E a execução fica "AGUARDANDO_EXECUCAO" na Execution
    Quando o mecânico inicia e conclui a execução
    Então a ordem fica "Finalizada" com motivo "SERVICO_CONCLUIDO"
    E a execução fica "CONCLUIDA" na Execution
    E a saga termina como "CONCLUIDA"

  Cenário: Cliente rejeita o orçamento e a execução é cancelada por compensação
    Quando o cliente rejeita o orçamento
    Então a ordem fica "Finalizada" com motivo "ORCAMENTO_RECUSADO"
    E a execução fica "CANCELADA" na Execution
    E a saga termina como "ENCERRADA_SEM_EXECUCAO"
    E a história da saga registra o comando "CancelarExecucao" e a confirmação "ExecucaoCancelada"

  Cenário: Cliente não decide no prazo e o orçamento e a execução são cancelados por compensação
    Quando o prazo de decisão vence sem resposta do cliente
    Então a ordem fica "Finalizada" com motivo "ORCAMENTO_EXPIRADO"
    E o orçamento fica "CANCELADO"
    E a execução fica "CANCELADA" na Execution
    E a saga termina como "ENCERRADA_SEM_EXECUCAO"
    E a história da saga registra o comando "CancelarOrcamento" e a confirmação "OrcamentoCancelado"
