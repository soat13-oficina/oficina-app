# Role do pod do OS Service via IRSA: consumir os-saga-replies, mandar comandos para Billing e
# Execution e enviar e-mail pelo SES. Substitui a role "<cluster>-ses-send-email" de
# oficina-infra-k8s, que so tinha SES.
#
# Nome previsivel - contrato com a pipeline, que o descobre com
#   aws iam get-role --role-name oficina-<env>-oficina-os-service

data "aws_iam_policy_document" "trust" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [local.platform.oidc_provider_arn]
    }

    # So a ServiceAccount deste servico, no namespace deste ambiente: o pod de hml nao assume a
    # role de prd.
    condition {
      test     = "StringEquals"
      variable = "${local.oidc_provider}:sub"
      values   = ["system:serviceaccount:${local.namespace}:${var.service_account}"]
    }

    condition {
      test     = "StringEquals"
      variable = "${local.oidc_provider}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "servico" {
  name               = "${var.project}-${local.environment}-${var.servico}"
  description        = "OS Service (${local.environment}): SQS da Saga e SES, assumida via IRSA"
  assume_role_policy = data.aws_iam_policy_document.trust.json
}

data "aws_iam_policy_document" "servico" {
  statement {
    sid = "ConsomeRespostasDaSaga"
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:ChangeMessageVisibility",
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes",
    ]
    resources = [aws_sqs_queue.os_saga_replies.arn]
  }

  # O ReceptorSqs manda direto para a DLQ o que esta fora do contrato.
  statement {
    sid       = "EnviaParaAPropriaDlq"
    actions   = ["sqs:SendMessage", "sqs:GetQueueUrl", "sqs:GetQueueAttributes"]
    resources = [aws_sqs_queue.os_saga_replies_dlq.arn]
  }

  # Filas criadas pelo infra/ do Billing e da Execution: ARN pelo nome, sem ler o state deles.
  statement {
    sid     = "EnviaComandosAosParticipantes"
    actions = ["sqs:SendMessage", "sqs:GetQueueUrl"]
    resources = [
      for fila in local.filas_de_comando :
      "arn:aws:sqs:${var.aws_region}:${local.conta_id}:${local.prefixo_filas}-${fila}.fifo"
    ]
  }

  statement {
    sid       = "EnviaEmail"
    actions   = ["ses:SendEmail"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "servico" {
  name   = "${var.project}-${local.environment}-${var.servico}"
  role   = aws_iam_role.servico.id
  policy = data.aws_iam_policy_document.servico.json
}
