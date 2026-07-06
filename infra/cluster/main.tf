# Provisiona um cluster Kubernetes local (kind = Kubernetes in Docker).
# Para apontar para um cluster cloud (EKS/GKE/AKS) em vez deste modulo,
# veja infra/README.md - o modulo infra/database aceita qualquer kubeconfig.
resource "kind_cluster" "this" {
  name           = var.cluster_name
  node_image     = var.node_image
  wait_for_ready = true

  kind_config {
    kind        = "Cluster"
    api_version = "kind.x-k8s.io/v1alpha4"

    node {
      role = "control-plane"
    }

    node {
      role = "worker"
    }
  }
}

# Kubeconfig gravado em disco para uso pelo modulo infra/database, pelo
# kubectl e pela pipeline de CI/CD.
resource "local_file" "kubeconfig" {
  content         = kind_cluster.this.kubeconfig
  filename        = "${path.module}/kubeconfig"
  file_permission = "0600"
}

provider "helm" {
  kubernetes {
    host                   = kind_cluster.this.endpoint
    client_certificate     = kind_cluster.this.client_certificate
    client_key             = kind_cluster.this.client_key
    cluster_ca_certificate = kind_cluster.this.cluster_ca_certificate
  }
}

# metrics-server: sem ele o Horizontal Pod Autoscaler (k8s/05-hpa.yaml) fica
# sem metricas de CPU/memoria e nunca escala. kubelet-insecure-tls e
# preferred-address-types sao necessarios pelo certificado self-signed e
# pela rede interna do kind.
resource "helm_release" "metrics_server" {
  count = var.install_metrics_server ? 1 : 0

  name       = "metrics-server"
  repository = "https://kubernetes-sigs.github.io/metrics-server/"
  chart      = "metrics-server"
  namespace  = "kube-system"
  wait       = true

  set {
    name  = "args[0]"
    value = "--kubelet-insecure-tls"
  }

  set {
    name  = "args[1]"
    value = "--kubelet-preferred-address-types=InternalIP"
  }

  depends_on = [kind_cluster.this]
}
