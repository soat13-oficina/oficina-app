output "cluster_name" {
  value = kind_cluster.this.name
}

output "endpoint" {
  value = kind_cluster.this.endpoint
}

output "kubeconfig_path" {
  description = "Caminho do kubeconfig gerado. Usado como default pelo modulo infra/database e pelo kubectl (--kubeconfig ou $env:KUBECONFIG)."
  value       = local_file.kubeconfig.filename
}
