output "namespace" {
  value = kubernetes_namespace.oficina.metadata[0].name
}

output "service_name" {
  description = "Nome DNS interno do banco: <service_name>.<namespace>.svc.cluster.local"
  value       = kubernetes_service.db.metadata[0].name
}

output "secret_name" {
  value = kubernetes_secret.db_credentials.metadata[0].name
}
