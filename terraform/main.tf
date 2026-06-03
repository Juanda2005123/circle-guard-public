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
  }
}

provider "kubernetes" {
  config_path    = "~/.kube/config"
  config_context = "kind-circleguard-cluster"
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

module "base_infrastructure" {
  source = "./modules/base-infrastructure"

  namespace_name = module.security.namespace_name
}

module "applications" {
  source = "./modules/applications"

  namespace_name = module.security.namespace_name
}
