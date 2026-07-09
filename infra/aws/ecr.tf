resource "aws_ecr_repository" "oficina" {
  name                 = var.ecr_repository_name
  image_tag_mutability = "MUTABLE"

  # Permite terraform destroy mesmo com imagens no repositorio (demo).
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "keep_last_10" {
  repository = aws_ecr_repository.oficina.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Mantem apenas as 10 imagens mais recentes"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}
