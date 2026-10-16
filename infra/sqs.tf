# Fila que o OS Service CONSOME: respostas e eventos dos participantes da Saga (ADR 0005). Cada
# servico cria as filas que consome; os produtores so precisam do nome.
#
# FIFO com MessageGroupId = ordemServicoId (ordem por OS) e MessageDeduplicationId = messageId
# (enviados pela aplicacao - por isso sem deduplicacao por conteudo).

resource "aws_sqs_queue" "os_saga_replies_dlq" {
  name                      = "${local.prefixo_filas}-os-saga-replies-dlq.fifo"
  fifo_queue                = true
  message_retention_seconds = local.cfg.retencao_dlq_segundos
  sqs_managed_sse_enabled   = true
}

resource "aws_sqs_queue" "os_saga_replies" {
  name                        = "${local.prefixo_filas}-os-saga-replies.fifo"
  fifo_queue                  = true
  content_based_deduplication = false
  message_retention_seconds   = local.cfg.retencao_mensagens_segundos
  sqs_managed_sse_enabled     = true

  # 60 s para processar antes de a mensagem voltar; long polling de 20 s no receive
  # (mensageria.sqs.espera). Valores documentados em docs/arquitetura/mensageria-local.md.
  visibility_timeout_seconds = 60
  receive_wait_time_seconds  = 20

  # Na 5a falha o SQS move para a DLQ. Fora do contrato a aplicacao manda direto (ReceptorSqs).
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.os_saga_replies_dlq.arn
    maxReceiveCount     = 5
  })
}

resource "aws_sqs_queue_redrive_allow_policy" "os_saga_replies_dlq" {
  queue_url = aws_sqs_queue.os_saga_replies_dlq.id

  redrive_allow_policy = jsonencode({
    redrivePermission = "byQueue"
    sourceQueueArns   = [aws_sqs_queue.os_saga_replies.arn]
  })
}
