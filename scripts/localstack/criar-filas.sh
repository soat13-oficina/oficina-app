#!/bin/bash
# Filas SQS FIFO da Saga no LocalStack (ADR 0005), com os mesmos atributos do Terraform:
# DLQ por fila (maxReceiveCount 5), visibility timeout de 60 s, long polling de 20 s.
# Executado pelo LocalStack quando fica pronto (/etc/localstack/init/ready.d).
set -euo pipefail

AMBIENTE="${AMBIENTE_FILAS:-local}"

for fila in billing-commands execution-commands os-saga-replies; do
  dlq="oficina-${AMBIENTE}-${fila}-dlq.fifo"
  principal="oficina-${AMBIENTE}-${fila}.fifo"

  awslocal sqs create-queue --queue-name "$dlq" \
    --attributes FifoQueue=true,ContentBasedDeduplication=false,MessageRetentionPeriod=1209600 >/dev/null
  arn_dlq=$(awslocal sqs get-queue-attributes \
    --queue-url "$(awslocal sqs get-queue-url --queue-name "$dlq" --query QueueUrl --output text)" \
    --attribute-names QueueArn --query Attributes.QueueArn --output text)

  awslocal sqs create-queue --queue-name "$principal" --attributes "{
    \"FifoQueue\": \"true\",
    \"ContentBasedDeduplication\": \"false\",
    \"VisibilityTimeout\": \"60\",
    \"ReceiveMessageWaitTimeSeconds\": \"20\",
    \"MessageRetentionPeriod\": \"345600\",
    \"RedrivePolicy\": \"{\\\"deadLetterTargetArn\\\":\\\"${arn_dlq}\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"
  }" >/dev/null

  echo "fila criada: ${principal} (DLQ ${dlq})"
done
