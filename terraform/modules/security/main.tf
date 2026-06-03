resource "kubernetes_namespace" "env_namespace" {
  metadata {
    name = var.namespace_name
    labels = {
      team = "dream-team"
    }
  }
}

resource "kubernetes_secret" "circleguard_secrets" {
  metadata {
    name      = "circleguard-secrets"
    namespace = kubernetes_namespace.env_namespace.metadata[0].name
    labels = {
      app  = "circleguard-secrets"
      team = "dream-team"
    }
  }

  type = "Opaque"

  data = {
    "postgres-username"   = var.postgres_username
    "postgres-password"   = var.postgres_password
    "neo4j-username"      = var.neo4j_username
    "neo4j-password"      = var.neo4j_password
    "neo4j-auth"          = var.neo4j_auth
    "ldap-admin-password" = var.ldap_admin_password
  }
}
