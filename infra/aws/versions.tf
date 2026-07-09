terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # Backend parcial: bucket e region vem de backend.hcl (nao versionado).
  #   terraform init -backend-config=backend.hcl
  # Ver infra/README.md para o bootstrap do bucket de state.
  backend "s3" {
    key          = "oficina/infra.tfstate"
    use_lockfile = true
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = "oficina"
      ManagedBy = "terraform"
    }
  }
}
