variable "cluster_name" {
  description = "Nome do cluster kind provisionado localmente."
  type        = string
  default     = "oficina"
}

variable "node_image" {
  description = "Imagem do node kind (define a versao do Kubernetes). null usa o default da versao do kind instalada."
  type        = string
  default     = null
}

variable "install_metrics_server" {
  description = "Instala o metrics-server via Helm. Necessario para o HPA em k8s/05-hpa.yaml funcionar."
  type        = bool
  default     = true
}
