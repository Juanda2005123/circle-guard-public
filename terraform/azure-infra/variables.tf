variable "resource_group_name" {
  type        = string
  description = "Name of the Azure resource group containing the cluster"
  default     = "circleguard-rg"
}

variable "location" {
  type        = string
  description = "Azure region"
  default     = "eastus2"
}

variable "cluster_name" {
  type        = string
  description = "Name of the AKS cluster"
  default     = "circleguard-aks"
}

variable "acr_name" {
  type        = string
  description = "Name of the Azure Container Registry (must be globally unique, alphanumeric only)"
  default     = "circleguardacr"
}

variable "node_count" {
  type        = number
  description = "Number of nodes in the default node pool"
  default     = 2
}

variable "node_vm_size" {
  type        = string
  description = "VM size for the default node pool"
  default     = "Standard_B2s"
}

variable "dns_prefix" {
  type        = string
  description = "DNS prefix for the AKS API server (must match the existing cluster's auto-generated prefix)"
  default     = "circleguar-circleguard-rg-0677c8"
}

variable "ssh_public_key" {
  type        = string
  description = "SSH public key for AKS node admin access"
  default     = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDFPy3mpHSEW7Ztm8eUvNtauboxULLp0rfriLTRdfRcE900K8ZXC0UYHnZ12gLON17FYWJOaOxvbZIrAltnDMNqfsgNaD7cjtfbkXEmbVW66BwIxjqxo/+Geew7kcP3GynAkX3bKrY9BCDD6h6/EMcC/u8FAtfqF+FEE6yYRAHm3jfTLranUgBBhAFkzexmPtUZKJ4lmzJnGbQldB3fZYlq/vgurGt/GHhMXNYuH0SKCAO0KRs5LsYX7AAFRZXFa3GuhSmYV/DZ4z2GWTjljLyeds754RfBrN8QCw6dz6kYw0R9VJb+8r+lm3RuakFAx/YFSxXmBZod16bkM63V5Pcf"
}
