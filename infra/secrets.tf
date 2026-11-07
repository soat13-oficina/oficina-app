# Token das chamadas entre servicos (header X-Service-Token nas rotas /internal/**).
#
# CONTRATO: as pipelines do OS (que valida) e do Billing (que chama /internal/pecas-insumos/consulta)
# leem o segredo pelo NOME "oficina/<ambiente>/service-token" e materializam a variavel
# SERVICE_TOKEN no Secret do Kubernetes. Renomear aqui quebra a consulta de precos do orcamento.

resource "random_password" "service_token" {
  length = 64
  # Sem caracteres especiais: o valor viaja por variavel de ambiente, Secret e header HTTP.
  special = false
}

resource "aws_secretsmanager_secret" "service_token" {
  name        = "${var.project}/${local.environment}/service-token"
  description = "Token das chamadas internas entre os servicos da oficina (${local.environment})"

  # Ambiente de desafio: sem isto, um destroy seguido de apply falharia por ~7 dias com
  # "secret scheduled for deletion".
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "service_token" {
  secret_id     = aws_secretsmanager_secret.service_token.id
  secret_string = random_password.service_token.result
}

# Segredo HMAC do JWT: a Lambda de autenticacao assina o token de cliente e os tres servicos validam.
#
# CONTRATO: as pipelines dos tres servicos e o infra/ do oficina-lambda-auth leem o segredo pelo NOME
# "oficina/<ambiente>/jwt-secret". Ficava no oficina-lambda-auth, mas o deploy do Billing e da
# Execution precisa dele e a Lambda precisa do ALB criado por esses deploys: aqui a ordem de subida
# fica linear (infra/ dos servicos -> deploy -> Lambda).
resource "random_password" "jwt" {
  length = 64
  # Sem caracteres especiais: o valor viaja por variavel de ambiente, Secret do Kubernetes e YAML.
  # 64 caracteres alfanumericos ja dao ~380 bits de entropia, acima dos 256 bits do HS256.
  special = false
}

resource "aws_secretsmanager_secret" "jwt" {
  name        = "${var.project}/${local.environment}/jwt-secret"
  description = "Segredo HMAC do JWT emitido pela Lambda de autenticacao e validado pelos servicos (${local.environment})"

  # Ambiente de desafio: sem isto, um destroy seguido de apply falharia por ~7 dias com
  # "secret scheduled for deletion".
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "jwt" {
  secret_id     = aws_secretsmanager_secret.jwt.id
  secret_string = random_password.jwt.result
}
