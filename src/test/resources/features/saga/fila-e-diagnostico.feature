# language: pt
Funcionalidade: Fila e diagnóstico na Execution Service
  Como oficina, quero que a ordem aberta entre na fila de diagnóstico do mecânico e que o orçamento
  nasça sozinho quando ele concluir o diagnóstico, sem que o atendente precise "enviar para orçamento"
  (ADR 0004, passos 1 e 2).

  Cenário: A ordem aberta entra na fila de diagnóstico
    Dado uma ordem de serviço aberta para o veículo "BDD3C45"
    Então a ordem fica "Recebida"
    E a execução fica "AGUARDANDO_DIAGNOSTICO" na Execution
    E a saga fica "AGUARDANDO_DIAGNOSTICO"
    E a história da saga registra o comando "EnfileirarDiagnostico" e a confirmação "DiagnosticoEnfileirado"
    E nenhum orçamento é gerado

  Cenário: O mecânico conclui o diagnóstico e o orçamento é gerado pela Saga
    Dado uma ordem de serviço aberta para o veículo "BDD4D56"
    Quando o mecânico inicia o diagnóstico
    Então a ordem fica "Diagnóstico"
    E a execução fica "EM_DIAGNOSTICO" na Execution
    Quando o mecânico conclui o diagnóstico com o serviço "Revisão completa"
    Então a ordem fica "Aguardando Aprovação"
    E a execução fica "DIAGNOSTICO_CONCLUIDO" na Execution
    E a saga fica "AGUARDANDO_DECISAO"
    E a história da saga registra o comando "GerarOrcamento" e a confirmação "OrcamentoGerado"
