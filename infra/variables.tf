variable "project" {
  description = "Nome do projeto, usado em tags e como prefixo de recursos."
  type        = string
  default     = "oficina"
}

variable "aws_region" {
  description = "Regiao AWS. Precisa ser a mesma da plataforma (oficina-infra-k8s)."
  type        = string
  default     = "us-east-1"
}

# A MESMA bucket do backend deste repositorio. O backend parcial nao expoe seu valor como
# variavel, por isso ele precisa ser informado explicitamente (-var, TF_VAR_state_bucket ou
# terraform.tfvars) para o terraform_remote_state.
variable "state_bucket" {
  description = "Bucket S3 com o state da plataforma, lido aqui via terraform_remote_state."
  type        = string
}

variable "platform_state_key" {
  description = "Key do state da plataforma. Deve casar com o backend de oficina-infra-k8s."
  type        = string
  default     = "oficina/infra-k8s.tfstate"
}

variable "servico" {
  description = "Nome logico do servico (docs/arquitetura/convencoes-de-nomes.md); compoe o nome da Role IRSA."
  type        = string
  default     = "oficina-os-service"
}

# Ainda "oficina-api": o Deployment, o Service e a ServiceAccount so mudam de nome no rename
# para oficina-os-service (item do k8s/ no PLANO-FASE-4.md).
variable "service_account" {
  description = "ServiceAccount do Kubernetes que assume a Role via IRSA."
  type        = string
  default     = "oficina-api"
}

# Rode "terraform workspace select hml" (ou prd) antes do plan/apply - o workspace "default" e
# rejeitado de proposito, ver locals.tf.
variable "environments" {
  description = "Configuracao por ambiente, indexada pelo nome do workspace do Terraform."
  type = map(object({
    # Quanto tempo uma resposta da Saga espera na fila antes de expirar.
    retencao_mensagens_segundos = number
    # Quanto tempo a DLQ guarda o que falhou, para analise e reprocessamento manual.
    retencao_dlq_segundos = number
  }))

  default = {
    hml = {
      retencao_mensagens_segundos = 345600 # 4 dias
      retencao_dlq_segundos       = 604800 # 7 dias
    }
    prd = {
      retencao_mensagens_segundos = 345600  # 4 dias
      retencao_dlq_segundos       = 1209600 # 14 dias (maximo do SQS)
    }
  }
}
