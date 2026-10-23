# Repositorio de imagens do OS Service. Um por ambiente, como no Billing e na Execution: com
# workspaces, um repositorio unico seria criado (e destruido) pelos dois - o destroy de hml levaria as
# imagens de prd junto. Substitui o ECR "oficina" que ficava no oficina-infra-k8s.
resource "aws_ecr_repository" "servico" {
  name                 = "${var.servico}-${local.environment}"
  image_tag_mutability = "MUTABLE" # tag movel <env>-latest
  force_delete         = true      # ambiente do desafio e destruido ao fim de cada demonstracao

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "servico" {
  repository = aws_ecr_repository.servico.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Guarda as 10 imagens mais recentes"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}
