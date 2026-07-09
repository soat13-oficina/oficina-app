resource "aws_security_group" "rds" {
  name_prefix = "${var.cluster_name}-rds-"
  description = "Acesso ao PostgreSQL apenas a partir dos nodes do EKS"
  vpc_id      = module.vpc.vpc_id

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "rds_from_nodes" {
  security_group_id            = aws_security_group.rds.id
  description                  = "PostgreSQL a partir dos worker nodes"
  referenced_security_group_id = module.eks.node_security_group_id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

resource "aws_db_instance" "oficina" {
  identifier = "${var.cluster_name}-db"

  engine         = "postgres"
  engine_version = "15"
  instance_class = var.db_instance_class

  allocated_storage = var.db_allocated_storage
  storage_type      = "gp3"

  db_name  = var.db_name
  username = var.db_username
  # Senha gerada e guardada pela AWS no Secrets Manager - nunca passa pelo
  # state do Terraform. A pipeline le de la para criar o Secret no cluster.
  manage_master_user_password = true

  db_subnet_group_name   = module.vpc.database_subnet_group_name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  # Configuracao de demo: sem backup automatico, sem snapshot final e sem
  # protecao de delete, para que apply/destroy sejam rapidos e baratos.
  # Em producao: backup_retention_period >= 7, deletion_protection = true.
  backup_retention_period = 0
  skip_final_snapshot     = true
  deletion_protection     = false
  apply_immediately       = true
}
