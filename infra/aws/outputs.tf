output "aws_region" {
  value = var.aws_region
}

output "cluster_name" {
  value = module.eks.cluster_name
}

output "configure_kubectl" {
  description = "Comando para apontar o kubectl para o cluster."
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

output "ecr_repository_url" {
  description = "URL do repositorio ECR para push/pull das imagens."
  value       = aws_ecr_repository.oficina.repository_url
}

output "rds_endpoint" {
  description = "Endpoint (host:porta) do PostgreSQL, usado no DB_URL da aplicacao."
  value       = "${aws_db_instance.oficina.address}:${aws_db_instance.oficina.port}"
}

output "rds_master_secret_arn" {
  description = "ARN do secret (Secrets Manager) com usuario/senha do banco, gerenciado pelo RDS."
  value       = aws_db_instance.oficina.master_user_secret[0].secret_arn
}

output "ses_role_arn" {
  description = "ARN da IAM Role (IRSA) com permissao ses:SendEmail, usada pelo ServiceAccount oficina-api."
  value       = aws_iam_role.ses_send_email.arn
}
