output "kube_config_raw" {
  value     = module.aks.kube_config_raw
  sensitive = true
}

output "acr_login_server" {
  value = module.aks.acr_login_server
}

output "acr_admin_username" {
  value = module.aks.acr_admin_username
}

output "acr_admin_password" {
  value     = module.aks.acr_admin_password
  sensitive = true
}

output "cluster_name" {
  value = module.aks.cluster_name
}
