# Namespace compartilhado entre o banco (provisionado aqui) e a aplicacao
# (manifestos em /k8s, aplicados depois via kubectl).
resource "kubernetes_namespace" "oficina" {
  metadata {
    name = var.namespace
  }
}

# Credenciais do banco. Consumidas tanto pelo StatefulSet do Postgres
# abaixo quanto pelo Deployment da API (k8s/03-deployment.yaml), que le
# as chaves POSTGRES_USER/POSTGRES_PASSWORD deste mesmo Secret.
resource "kubernetes_secret" "db_credentials" {
  metadata {
    name      = "oficina-db-credentials"
    namespace = kubernetes_namespace.oficina.metadata[0].name
  }

  data = {
    POSTGRES_USER     = var.db_user
    POSTGRES_PASSWORD = var.db_password
    POSTGRES_DB       = var.db_name
  }

  type = "Opaque"
}

# Service headless: da nome de rede estavel ao StatefulSet
# (oficina-db.oficina.svc.cluster.local, referenciado no ConfigMap da API).
resource "kubernetes_service" "db" {
  metadata {
    name      = "oficina-db"
    namespace = kubernetes_namespace.oficina.metadata[0].name
    labels = {
      app = "oficina-db"
    }
  }

  spec {
    selector = {
      app = "oficina-db"
    }

    port {
      name        = "postgres"
      port        = 5432
      target_port = 5432
    }

    cluster_ip = "None"
  }
}

resource "kubernetes_stateful_set" "db" {
  metadata {
    name      = "oficina-db"
    namespace = kubernetes_namespace.oficina.metadata[0].name
    labels = {
      app = "oficina-db"
    }
  }

  spec {
    service_name = kubernetes_service.db.metadata[0].name
    replicas     = 1

    selector {
      match_labels = {
        app = "oficina-db"
      }
    }

    template {
      metadata {
        labels = {
          app = "oficina-db"
        }
      }

      spec {
        container {
          name  = "postgres"
          image = var.postgres_image

          port {
            container_port = 5432
          }

          env_from {
            secret_ref {
              name = kubernetes_secret.db_credentials.metadata[0].name
            }
          }

          # sub_path isola os dados em uma subpasta do volume, evitando
          # problemas com diretorios como lost+found em provisioners reais
          # (EBS, etc.) que nao aparecem no local-path do kind.
          volume_mount {
            name       = "data"
            mount_path = "/var/lib/postgresql/data"
            sub_path   = "postgres"
          }

          resources {
            requests = {
              cpu    = "250m"
              memory = "256Mi"
            }
            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
          }

          readiness_probe {
            exec {
              command = ["pg_isready", "-U", var.db_user]
            }
            initial_delay_seconds = 10
            period_seconds        = 10
          }

          liveness_probe {
            exec {
              command = ["pg_isready", "-U", var.db_user]
            }
            initial_delay_seconds = 30
            period_seconds        = 20
          }
        }
      }
    }

    volume_claim_template {
      metadata {
        name = "data"
      }
      spec {
        access_modes       = ["ReadWriteOnce"]
        storage_class_name = var.storage_class_name
        resources {
          requests = {
            storage = var.storage_size
          }
        }
      }
    }
  }
}
