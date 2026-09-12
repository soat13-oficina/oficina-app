# RFC 0002 — Escolha do banco de dados

- **Status:** Aceito
- **Data:** 2026-09-12
- **Escopo:** `oficina-infra-database`, `oficina-app`, `oficina-lambda-auth`

## Contexto

O enunciado exige **banco de dados gerenciado** e deixa a tecnologia livre
(PostgreSQL, MySQL, SQL Server ou outro), pedindo ainda "melhorar e documentar a
modelagem, garantindo consistência e performance".

O domínio é de uma oficina mecânica: clientes, veículos, funcionários, ordens de
serviço, orçamentos, peças e controle de estoque. O modelo herdado da Fase 2 tem
**13 tabelas** e 11 chaves estrangeiras — ver
[`../arquitetura/modelo-de-dados.md`](../arquitetura/modelo-de-dados.md).

## Proposta

**Amazon RDS for PostgreSQL 15**, uma instância `db.t3.micro` por ambiente, em
subnets dedicadas de banco, sem acesso público, com senha gerada e custodiada
pelo AWS Secrets Manager.

## Alternativas consideradas

### Paradigma: relacional × documento

| Alternativa | Avaliação |
|---|---|
| **Relacional** (escolhido) | O domínio é fortemente relacional: quase toda consulta cruza cliente, veículo, OS e peça. As junções são o caso de uso, não a exceção. |
| **Documento** (DynamoDB, MongoDB) | Exigiria duplicar dados entre agregados e resolver junções na aplicação. Pior: a abertura de OS reserva estoque **e** persiste a ordem numa única transação — sem ACID isso vira saga com compensação manual, complexidade desproporcional ao problema. |

### Motor: PostgreSQL × MySQL × SQL Server

| Alternativa | Avaliação |
|---|---|
| **PostgreSQL** (escolhido) | Tipo `uuid` nativo — usado em 8 dos 9 PKs do schema. `numeric` de precisão exata para valores monetários. **Índices funcionais**, que foram o que resolveu a busca de CPF normalizado sem migração de dados. Sem custo de licença. |
| **MySQL** | Atenderia, mas sem `uuid` nativo (armazenado como `char(36)` ou `binary(16)`) e sem índice funcional até o 8.0.13, que foi exatamente o recurso de que precisamos. |
| **SQL Server** | Custo de licença no RDS, desproporcional para o projeto, sem ganho técnico no domínio. |

### Hospedagem: RDS × Aurora × contêiner no cluster

| Alternativa | Avaliação |
|---|---|
| **RDS gerenciado** (escolhido) | Exigido pelo enunciado, e a razão técnica acompanha: backup, patch, failover e rotação de credencial saem prontos. `manage_master_user_password` mantém a senha fora do state do Terraform. |
| **Aurora PostgreSQL** | Melhor desempenho e failover mais rápido, mas a instância mínima custa várias vezes o `db.t3.micro`, sem ganho perceptível no volume de uma demonstração. |
| **PostgreSQL em pod no EKS** | Mais barato e descumpre o enunciado. Além disso colocaria o dado no mesmo domínio de falha da aplicação: um problema no cluster levaria o banco junto. |

## Configuração por ambiente

Tudo o que varia está num único `map(object)` no Terraform, o que torna a
diferença entre ambientes revisável num diff:

| | `hml` | `prd` |
|---|---|---|
| Classe | `db.t3.micro` | `db.t3.micro` |
| Storage | 20 GiB gp3 criptografado, autoscaling até 50 GiB | idem, até 100 GiB |
| Retenção de backup | 0 dia | **7 dias** (PITR) |
| Multi-AZ | não | não |

**Instâncias separadas, e não dois databases lógicos na mesma.** Custa US$ 0,017/h
a mais e compra isolamento real: saturação de CPU ou disco em homologação não
alcança produção. A alternativa exigiria o provider `postgresql` no Terraform ou
um `null_resource` com `psql`, e colocaria os dois ambientes no mesmo domínio de
falha.

## Impacto

**Positivo**

- Integridade referencial e transação garantidas pelo banco, não por código.
- Índice funcional resolveu a busca por documento normalizado sem migrar dado existente.
- Nenhuma credencial de banco em código, state ou log: o RDS gera a senha, o Secrets Manager guarda, a pipeline lê no momento do deploy.
- Identificador previsível (`oficina-db-<ambiente>`) como contrato entre repositórios, evitando que a aplicação precise ler o state do Terraform do banco.

**Negativo e mitigações**

- **Single-AZ, sem réplica de leitura.** Produção real pediria `multi_az = true`; aqui dobraria o custo sem agregar à demonstração. Decisão de custo explícita, reversível por uma variável.
- **`deletion_protection = false` e sem snapshot final**, inclusive em produção. Deliberado: o ambiente é destruído ao fim de cada sessão para não acumular custo. Em produção real, os dois seriam o inverso.
- **Custo:** ~US$ 1,00/dia com as duas instâncias ligadas.

## Questões em aberto

- **Sete chaves estrangeiras sem índice.** Diagnóstico pronto e SQL escrito em [`../arquitetura/modelo-de-dados.md`](../arquitetura/modelo-de-dados.md); é o ajuste de maior impacto e menor risco ainda não aplicado.
- **Unicidade do documento do cliente é frágil.** `uk_clientes_cpf_ou_cnpj` compara a string bruta, então o mesmo CPF com e sem máscara passa como dois clientes. Corrigir exige normalizar na escrita e desduplicar o que já existe — decisão de negócio, não só técnica.
