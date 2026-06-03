variable "namespace_name" {
  type        = string
  description = "The name of the Kubernetes namespace to deploy applications"
}

variable "image_tag" {
  type        = string
  description = "The Docker image tag to deploy for the services"
  default     = "latest"
}

variable "docker_registry" {
  type        = string
  description = "The Docker registry where images are stored"
  default     = "localhost:5000"
}
