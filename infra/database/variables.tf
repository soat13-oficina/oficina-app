variable "kubeconfig_path" {
  description = "Kubeconfig do cluster onde o banco sera provisionado. Default aponta para o arquivo gerado pelo modulo infra/cluster (kind local). Para cloud, aponte para o seu proprio kubeconfig (ex.: gerado por 'aws eks update-kubeconfig')."
  type        = string
  default     = "../cluster/kubeconfig"
}

variable "namespace" {
  description = "Namespace onde o banco e a aplicacao (k8s/) serao provisionados."
  type        = string
  default     = "oficina"
}

variable "db_name" {
  type    = string
  default = "oficina_db"
}

variable "db_user" {
  type    = string
  default = "oficina_user"
}

variable "db_password" {
  description = "Senha do banco. Valor default apenas para demo local/CI - sobrescreva com -var, TF_VAR_db_password ou um *.tfvars nao versionado em qualquer uso real."
  type        = string
  default     = "oficina_123"
  sensitive   = true
}

variable "storage_size" {
  description = "Tamanho do volume persistente do Postgres."
  type        = string
  default     = "1Gi"
}

variable "storage_class_name" {
  description = "StorageClass do PVC. null usa a StorageClass default do cluster (no kind, 'standard')."
  type        = string
  default     = null
}

variable "postgres_image" {
  type    = string
  default = "postgres:15-alpine"
}
