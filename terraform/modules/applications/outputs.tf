output "gateway_service_name" {
  description = "The name of the Gateway Kubernetes Service"
  value       = kubernetes_service.gateway_service.metadata[0].name
}

output "gateway_node_port" {
  description = "The NodePort exposed by the Gateway Service"
  value       = kubernetes_service.gateway_service.spec[0].port[0].node_port
}
