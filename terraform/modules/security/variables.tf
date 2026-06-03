variable "namespace_name" {
  type        = string
  description = "The name of the Kubernetes namespace to create"
}

variable "postgres_username" {
  type        = string
  sensitive   = true
  description = "The username for PostgreSQL"
}

variable "postgres_password" {
  type        = string
  sensitive   = true
  description = "The password for PostgreSQL"
}

variable "neo4j_username" {
  type        = string
  sensitive   = true
  description = "The username for Neo4j"
}

variable "neo4j_password" {
  type        = string
  sensitive   = true
  description = "The password for Neo4j"
}

variable "neo4j_auth" {
  type        = string
  sensitive   = true
  description = "The combined auth string for Neo4j (username/password)"
}

variable "ldap_admin_password" {
  type        = string
  sensitive   = true
  description = "The admin password for OpenLDAP"
}
