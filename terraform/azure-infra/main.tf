terraform {
  required_version = ">= 1.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.100"
    }
  }

  backend "azurerm" {
    resource_group_name  = "circleguard-rg"
    storage_account_name = "circleguardtfstate"
    container_name       = "tfstate"
    key                  = "azure-infra.tfstate"
  }
}

provider "azurerm" {
  features {}
}

module "aks" {
  source = "./modules/aks"

  resource_group_name = var.resource_group_name
  location            = var.location
  cluster_name        = var.cluster_name
  dns_prefix          = var.dns_prefix
  acr_name            = var.acr_name
  node_count          = var.node_count
  node_vm_size        = var.node_vm_size
  ssh_public_key      = var.ssh_public_key
}
