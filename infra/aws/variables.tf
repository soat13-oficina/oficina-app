variable "aws_region" {
  description = "Regiao AWS onde toda a infraestrutura sera provisionada."
  type        = string
  default     = "us-east-1"
}

variable "cluster_name" {
  description = "Nome do cluster EKS. Tambem usado como prefixo dos demais recursos."
  type        = string
  default     = "oficina"
}

variable "kubernetes_version" {
  description = "Versao do Kubernetes no EKS. Mantenha uma versao em standard support - versoes antigas caem em extended support e o control plane custa 6x mais."
  type        = string
  default     = "1.34"
}

variable "node_instance_type" {
  description = "Tipo de instancia dos worker nodes."
  type        = string
  default     = "t3.small"
}

variable "node_min_size" {
  type    = number
  default = 2
}

variable "node_max_size" {
  type    = number
  default = 4
}

variable "node_desired_size" {
  type    = number
  default = 2
}

variable "db_instance_class" {
  description = "Classe da instancia RDS."
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "Armazenamento do RDS em GiB."
  type        = number
  default     = 20
}

variable "db_name" {
  type    = string
  default = "oficina_db"
}

variable "db_username" {
  type    = string
  default = "oficina_user"
}

variable "ecr_repository_name" {
  type    = string
  default = "oficina"
}
