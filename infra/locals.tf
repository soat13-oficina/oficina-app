locals {
  environment = terraform.workspace

  # Falha cedo e com mensagem legivel quando alguem esquece de selecionar o workspace - sem isto,
  # um apply no workspace default criaria filas fora de qualquer ambiente.
  cfg = try(
    var.environments[local.environment],
    file("ERRO: workspace '${terraform.workspace}' invalido. Rode 'terraform workspace select hml' ou 'terraform workspace select prd' antes do plan/apply.")
  )

  namespace = "${var.project}-${local.environment}"

  # Nome fisico das filas: oficina-<ambiente>-<fila>.fifo (ADR 0005). O OS descobre a URL pelo
  # nome (GetQueueUrl), entao este padrao e contrato com a aplicacao (mensageria.sqs.ambiente).
  prefixo_filas = "${var.project}-${local.environment}"

  # Filas de outros servicos para onde o OS manda comandos. Cada consumidor cria a sua no proprio
  # infra/; aqui so entra o ARN, montado pelo nome, para a policy de envio.
  filas_de_comando = ["billing-commands", "execution-commands"]

  # Emissor OIDC do cluster sem "https://", como as condicoes da trust policy exigem.
  oidc_provider = element(split("oidc-provider/", local.platform.oidc_provider_arn), 1)
}
