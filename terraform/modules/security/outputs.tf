output "namespace_name" {
  description = "The name of the created Kubernetes namespace"
  value       = kubernetes_namespace.env_namespace.metadata[0].name
}

output "secret_name" {
  description = "The name of the created Kubernetes secret"
  value       = kubernetes_secret.circleguard_secrets.metadata[0].name
}
