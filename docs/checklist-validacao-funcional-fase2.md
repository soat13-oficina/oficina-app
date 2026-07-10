# Checklist de Validação Funcional — Fase 2 Codebase

Este checklist cobre a parte de codificação exigida no PDF `docs/14SOAT - Fase 2 - Tech challenge.pdf`: evolução da OS, consulta de status, decisão externa de orçamento, listagem priorizada e notificação por e-mail. Use a collection `docs/collections/oficina-api.insomnia.json` para executar os cenários de caixa preta.

## Pré-condições

- [ ] Banco local iniciado e aplicação respondendo em `{{ _.base_url }}`.
- [ ] Usuário criado e login realizado; variável `token` preenchida com o JWT.
- [ ] Variável `webhook_token` preenchida com o mesmo valor de `ORCAMENTO_WEBHOOK_SECRET` do ambiente local.
- [ ] Cliente PF criado com e-mail; variável `cliente_id` preenchida.
- [ ] Funcionário criado; variável `funcionario_id` preenchida.
- [ ] Veículo criado para o cliente; variável `veiculo_placa` preenchida.
- [ ] Peça/insumo criada com estoque suficiente; variável `peca_id` preenchida.

## Abertura de OS

- [ ] Executar `Ordens de Serviço / Criar OS` com `clienteId`, `funcionarioId`, `placaVeiculo`, `servicos` e `pecasPrevistas`.
  - Esperado: `201 Created`, header `Location` e corpo com `numeroOrdemServico` e `situacao = "Recebida"`.
  - Registrar o número retornado em `numero_os`.
- [ ] Executar `Ordens de Serviço / Consultar status da OS`.
  - Esperado: `200 OK`, `situacao = "Recebida"`, dados do cliente e placa corretos.
- [ ] Repetir abertura com peça inexistente.
  - Esperado: `400 Bad Request` com mensagem clara; nenhuma OS criada.
- [ ] Repetir abertura com veículo que não pertence ao cliente.
  - Esperado: `400 Bad Request` com mensagem clara; nenhuma OS criada.

## Ciclo de Vida e Status

- [ ] Executar `Iniciar diagnóstico`.
  - Esperado: `204 No Content`; status posterior `Diagnóstico`.
- [ ] Executar `Concluir diagnóstico`.
  - Esperado: `204 No Content`; status permanece coerente para envio a orçamento.
- [ ] Executar `Enviar diagnóstico para orçamento`.
  - Esperado: `204 No Content`; consultar orçamento e preencher `numero_orcamento`.
- [ ] Executar `Enviar OS para aprovação` quando aplicável ao fluxo preparado.
  - Esperado: `204 No Content`; status posterior `Aguardando Aprovação`.
- [ ] Tentar uma transição inválida, por exemplo `Concluir serviço` antes de aprovação.
  - Esperado: `400 Bad Request` com mensagem de transição inválida.

## Webhook de Decisão de Orçamento

- [ ] Com a OS em `Aguardando Aprovação`, executar `Integração externa - Orçamento / Webhook - Aprovar orçamento`.
  - Esperado: `200 OK`, corpo com `numeroOrcamento`, `numeroOrdemServico` e `situacao = "Execução"`.
- [ ] Executar `Consultar status da OS`.
  - Esperado: `situacao = "Execução"`.
- [ ] Executar `Webhook - Retry idempotente aprovação`.
  - Esperado: `200 OK`, mesma situação, sem erro e sem reprocessamento visível.
- [ ] Executar `Webhook - Decisão divergente` após aprovação.
  - Esperado: `400 Bad Request` com mensagem clara.
- [ ] Em outra OS/orçamento em `Aguardando Aprovação`, executar `Webhook - Rejeitar orçamento`.
  - Esperado: `200 OK`, `situacao = "Finalizada"`.
- [ ] Consultar status da OS rejeitada.
  - Esperado: `situacao = "Finalizada"` e `motivoEncerramento = "ORCAMENTO_RECUSADO"`.
- [ ] Executar `Webhook - Token inválido`.
  - Esperado: `401 Unauthorized`.
- [ ] Executar `Webhook - Sem token`.
  - Esperado: `401 Unauthorized`.

## Execução, Finalização e Entrega

- [ ] Para OS aprovada em `Execução`, executar `Concluir serviço`.
  - Esperado: `204 No Content`; status posterior `Finalizada`, `motivoEncerramento = "SERVICO_CONCLUIDO"`.
- [ ] Executar `Entregar ao cliente`.
  - Esperado: `204 No Content`; status posterior `Entregue`.
- [ ] Consultar acompanhamento público da OS com `documentoCliente`.
  - Esperado: `200 OK` com situação atual compatível.

## Listagem Priorizada

- [ ] Criar ou reutilizar OS nas situações `Execução`, `Aguardando Aprovação`, `Diagnóstico` e `Recebida`.
- [ ] Executar `Ordens de Serviço / Consultar ordens de serviço` sem filtros.
  - Esperado: lista com OS ativas ordenadas por `Execução > Aguardando Aprovação > Diagnóstico > Recebida`.
- [ ] Validar múltiplas OS na mesma situação.
  - Esperado: mais antigas aparecem antes das mais recentes.
- [ ] Validar que OS `Finalizada` e `Entregue` não aparecem na listagem operacional.

## Notificação por E-mail

- [ ] Com cliente contendo e-mail, executar qualquer transição de status.
  - Esperado: transição concluída mesmo que o envio de e-mail não esteja disponível localmente.
- [ ] Validar logs da aplicação após a transição.
  - Esperado: registro de tentativa ou falha de notificação sem rollback da OS.
- [ ] Repetir com cliente sem e-mail cadastrado.
  - Esperado: transição concluída e ausência de envio registrada sem erro funcional.

## Regressões Básicas

- [ ] Consultar OS inexistente.
  - Esperado: `404 Not Found` com mensagem clara.
- [ ] Consultar orçamento inexistente no webhook com token válido.
  - Esperado: `404 Not Found`.
- [ ] Executar endpoint protegido sem JWT, exceto webhook/acompanhamento público.
  - Esperado: `401 Unauthorized` ou bloqueio equivalente.
- [ ] Validar que respostas de erro não expõem stacktrace, token, senha ou detalhes internos.
