output "environment" {
  description = "Ambiente correspondente ao workspace selecionado (hml ou prd)."
  value       = local.environment
}

output "irsa_role_arn" {
  description = "Role do pod (anotacao eks.amazonaws.com/role-arn da ServiceAccount)."
  value       = aws_iam_role.servico.arn
}

output "fila_os_saga_replies" {
  description = "URL da fila que o OS consome."
  value       = aws_sqs_queue.os_saga_replies.url
}

output "fila_os_saga_replies_dlq" {
  description = "URL da DLQ das respostas da Saga."
  value       = aws_sqs_queue.os_saga_replies_dlq.url
}

output "service_token_secret_name" {
  description = "Nome do segredo do token interno, lido pelas pipelines do OS e do Billing."
  value       = aws_secretsmanager_secret.service_token.name
}

output "jwt_secret_name" {
  description = "Nome do segredo do JWT, lido pelas pipelines dos tres servicos e pelo oficina-lambda-auth."
  value       = aws_secretsmanager_secret.jwt.name
}

output "ecr_repository_url" {
  description = "Repositorio das imagens do OS neste ambiente."
  value       = aws_ecr_repository.servico.repository_url
}
