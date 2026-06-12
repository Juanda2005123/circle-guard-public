output "namespace_name" {
  description = "The Kubernetes namespace the secret was applied to"
  value       = var.namespace_name
}

output "secret_name" {
  description = "The name of the created Kubernetes secret"
  value       = kubernetes_secret.circleguard_secrets.metadata[0].name
}
