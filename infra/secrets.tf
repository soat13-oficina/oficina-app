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
