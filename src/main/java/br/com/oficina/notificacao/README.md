# Domínio: Notificacao

## Objetivo

Notificar clientes por email quando o status de sua ordem de serviço é alterado, de forma
assíncrona e resiliente a falhas — uma falha de envio nunca interrompe o fluxo principal
da OS.

## Responsabilidade Principal

Escutar o evento de domínio `StatusOrdemDeServicoAlterado` publicado pelo módulo
`ordemservico` e enviar email ao cliente (se possuir endereço cadastrado) com a nova
situação da OS.

## Funcionalidades Implementadas

### Use Cases / Operações

| Operação | Tipo | Descrição |
|---|---|---|
| `EnviarNotificacaoStatusOS` | Listener de evento assíncrono | Reage ao evento `StatusOrdemDeServicoAlterado`; envia email ao cliente com a nova situação |

### Fluxo de Notificação

```
[ordemservico publica StatusOrdemDeServicoAlterado]
           |
           | ApplicationEventPublisher (Spring)
           ▼
EnviarNotificacaoStatusOSService (@EventListener, @Async)
           |
           ├── ClienteRepository.buscarPorId(clienteId)
           │         ├── [não encontrado] → log.warn, sem envio
           │         └── [encontrado]
           │                  ├── cliente.getEmail() == null → log.info, sem envio
           │                  └── [email presente] → NotificadorEmail.enviar(...)
           |
           └── [RuntimeException] → log.warn, sem relançamento
```

### Regras de Negócio

1. **Silêncio em ausência de email** — se o cliente não possui email cadastrado, nenhum
   envio é feito; o evento é registrado em log info.
2. **Silêncio em ausência de cliente** — se o `clienteId` do evento não corresponde a um
   cliente existente, o evento é descartado com log de aviso.
3. **Resiliência a falhas** — toda `RuntimeException` na cadeia de notificação é capturada
   e logada como aviso; o thread de negócio que gerou o evento não é impactado.
4. **Assíncrono** — a execução ocorre em thread separada (`@Async`), desacoplando a latência
   do envio de email do tempo de resposta das APIs.

## Ciclo de Vida

O módulo não possui entidade própria nem ciclo de vida de dados. É puramente reativo.

```
[StatusOrdemDeServicoAlterado] ──► [EnviarNotificacaoStatusOSService] ──► [Email enviado]
                                                                      └──► [Email não enviado (sem endereço ou erro)]
```

## Dependências Internas

| Módulo | Direção | Detalhe |
|---|---|---|
| `ordemservico` | `notificacao` consome evento de `ordemservico` | Escuta `StatusOrdemDeServicoAlterado` publicado por `EnviarDiagnosticoParaOrcamentoService`, `DecidirOrcamentoExternamenteService` e outros services de transição de estado |
| `cliente` | `notificacao` consome `cliente` | `ClienteRepository.buscarPorId()` para obter o email do destinatário |

## Dependências Externas

- **Spring Mail (JavaMailSender)** — envio de emails SMTP via `SimpleMailMessage`
- **Spring `@Async` + `@EventListener`** — desacoplamento assíncrono do fluxo de negócio

## Pontos de Entrada REST

Nenhum. O módulo não expõe endpoints REST — é acionado exclusivamente por eventos internos.

## Modelos de Domínio

Nenhuma entidade própria. O módulo consome dados de outros domínios via evento e
repositório.

### `StatusOrdemDeServicoAlterado` — Evento consumido (definido em `ordemservico`)

| Campo | Tipo | Descrição |
|---|---|---|
| `numeroOrdemServico` | `String` | Número da OS cujo status mudou |
| `clienteId` | `UUID` | ID do cliente para busca do email |
| `situacaoAnterior` | `SituacaoOrdemDeServico` | Situação antes da transição |
| `novaSituacao` | `SituacaoOrdemDeServico` | Situação após a transição (exibida no email) |
| `ocorridoEm` | `LocalDateTime` | Timestamp da mudança |

## Arquivos Críticos

| Arquivo | Responsabilidade |
|---|---|
| `notificacao/application/EnviarNotificacaoStatusOSService.java` | Listener assíncrono do evento; orquestra busca do cliente e envio |
| `notificacao/application/NotificadorEmail.java` | Port (interface) do adaptador de email |
| `notificacao/infrastructure/NotificadorEmailSpringMail.java` | Adapter Spring Mail; envia via `SimpleMailMessage` |

## Observações

- **`SimpleMailMessage` sem HTML**: os emails são texto plano. O corpo da mensagem é
  `"Sua ordem de servico {numero} agora esta em: {descricao}."` — sem template, sem
  formatação rica.
- **Configuração de SMTP via `application.yml`**: `spring.mail.*` deve ser configurado
  com servidor SMTP externo para funcionar em produção. Em ambiente de desenvolvimento,
  a ausência de configuração causa `MailException` que é capturada pelo bloco `try/catch`
  do service — o sistema não quebra, apenas loga o aviso.
- **Sem retry**: se o envio falhar (SMTP indisponível, timeout), o evento é descartado. Não
  há mecanismo de reenvio ou fila de mensagens pendentes.

---

## Pontos de Atenção

### Achados de Integridade Crítica

#### [CRI-001] Sem retry e sem persistência de notificações pendentes

**Componente afetado**: `EnviarNotificacaoStatusOSService`

**Descrição**: falhas de envio de email (SMTP indisponível, timeout, endereço inválido)
são silenciadas pelo bloco `catch (RuntimeException)`. O evento não é reenfileirado nem
armazenado para reprocessamento posterior. Em produção, com servidor SMTP instável ou
janela de manutenção, notificações são descartadas permanentemente sem rastreabilidade
além do log de aviso.

**Impacto**: clientes não recebem notificações em cenários de falha transiente de
infraestrutura; não há forma de identificar quantas notificações foram perdidas ou
reenviar manualmente.

**Correção sugerida**: implementar uma tabela de notificações pendentes (`notificacoes`)
com status (`PENDENTE`, `ENVIADA`, `FALHOU`) e um job de reprocessamento, ou adotar uma
fila de mensagens (ex.: RabbitMQ, SQS) para garantia de entrega.

---

### Achados de Melhoria

#### [MEL-001] Módulo sem camada `domain` formal

**Componente afetado**: estrutura de pacotes `notificacao`

**Descrição**: o módulo possui apenas `application` e `infrastructure`, sem a camada
`domain` obrigatória pela constituição (Princípio I). `NotificadorEmail` está no pacote
`application` em vez de `domain`. O módulo também não possui entidades, repositórios nem
use cases formais.

**Sugestão de backlog**: mover `NotificadorEmail` para `notificacao/domain/port/` e
considerar introduzir entidade `Notificacao` para rastreabilidade (ver CRI-001).

---

#### [MEL-002] Conteúdo do email hardcoded sem template

**Componente afetado**: `EnviarNotificacaoStatusOSService#enviarSePossuirEmail`

**Descrição**: o assunto e o corpo do email são construídos por concatenação direta no
service — sem template, internacionalização ou configuração externalizada.

**Sugestão de backlog**: extrair o template de email para um arquivo de recurso
(ex.: `templates/notificacao-status-os.txt`) ou usar Thymeleaf para emails HTML.

---

#### [MEL-003] Ausência de testes unitários

**Componente afetado**: módulo completo `notificacao`

**Descrição**: não há testes para `EnviarNotificacaoStatusOSService`. O comportamento de
silêncio em ausência de email, de cliente ou em caso de falha não está verificado por
testes automatizados.

**Sugestão de backlog**: adicionar testes unitários cobrindo os três cenários: cliente sem
email, cliente inexistente e falha de `NotificadorEmail`.
