#!/bin/bash
# Tabelas DynamoDB do Execution Service no LocalStack (ADR 0006), com o mesmo desenho do Terraform do
# oficina-execution-service. So para o desenvolvimento local: em nuvem a aplicacao nunca cria tabela.
# Executado pelo LocalStack quando fica pronto (/etc/localstack/init/ready.d), depois de criar-filas.sh.
# O desenho das tabelas tem fonte unica em oficina-execution-service/scripts/criar-tabelas-local.sh.
set -euo pipefail

AMBIENTE="${AMBIENTE_FILAS:-local}"

criar() {
  local nome="$1"; shift
  awslocal dynamodb create-table --table-name "$nome" --billing-mode PAY_PER_REQUEST "$@" >/dev/null
  echo "tabela criada: $nome"
}

criar "oficina-${AMBIENTE}-pecas" \
  --attribute-definitions AttributeName=pecaId,AttributeType=S \
  --key-schema AttributeName=pecaId,KeyType=HASH

criar "oficina-${AMBIENTE}-reservas" \
  --attribute-definitions AttributeName=ordemServicoId,AttributeType=S AttributeName=pecaId,AttributeType=S \
  --key-schema AttributeName=ordemServicoId,KeyType=HASH AttributeName=pecaId,KeyType=RANGE

criar "oficina-${AMBIENTE}-execucoes" \
  --attribute-definitions AttributeName=ordemServicoId,AttributeType=S AttributeName=etapa,AttributeType=S AttributeName=entrouNaEtapaEm,AttributeType=S \
  --key-schema AttributeName=ordemServicoId,KeyType=HASH \
  --global-secondary-indexes 'IndexName=fila-por-etapa,KeySchema=[{AttributeName=etapa,KeyType=HASH},{AttributeName=entrouNaEtapaEm,KeyType=RANGE}],Projection={ProjectionType=ALL}'

criar "oficina-${AMBIENTE}-execution-controle" \
  --attribute-definitions AttributeName=pk,AttributeType=S AttributeName=pendente,AttributeType=S AttributeName=criadoEm,AttributeType=S \
  --key-schema AttributeName=pk,KeyType=HASH \
  --global-secondary-indexes 'IndexName=outbox-pendente,KeySchema=[{AttributeName=pendente,KeyType=HASH},{AttributeName=criadoEm,KeyType=RANGE}],Projection={ProjectionType=ALL}'

awslocal dynamodb update-time-to-live --table-name "oficina-${AMBIENTE}-execution-controle" \
  --time-to-live-specification Enabled=true,AttributeName=expiraEm >/dev/null
