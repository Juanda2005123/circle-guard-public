terraform {
  required_version = ">= 1.0"

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.26"
    }
  }

  backend "kubernetes" {
    secret_suffix = "circleguard-state"
    config_path   = "~/.kube/config"
    config_context = "circleguard-aks"
  }
}

provider "kubernetes" {
  config_path    = "~/.kube/config"
  config_context = "circleguard-aks"
}

module "security" {
  source = "./modules/security"

  namespace_name      = var.namespace_name
  postgres_username   = var.postgres_username
  postgres_password   = var.postgres_password
  neo4j_username      = var.neo4j_username
  neo4j_password      = var.neo4j_password
  neo4j_auth          = var.neo4j_auth
  ldap_admin_password = var.ldap_admin_password
}

# Los módulos base-infrastructure y applications no se usan: la
# infraestructura y los microservicios ya se despliegan vía
# Jenkins (kubectl apply + envsubst sobre k8s/infrastructure y
# k8s/templates). Mantenerlos aquí duplicaría esos recursos y
# entraría en conflicto con los que ya gestiona el pipeline.
