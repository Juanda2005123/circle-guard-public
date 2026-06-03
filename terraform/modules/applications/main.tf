# ============================================================
# 1. Gateway Service
# ============================================================

resource "kubernetes_deployment" "gateway_service" {
  metadata {
    name      = "gateway-service-deployment"
    namespace = var.namespace_name
    labels = {
      app     = "gateway-service"
      team    = "dream-team"
      version = var.image_tag
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "gateway-service"
      }
    }

    template {
      metadata {
        labels = {
          app     = "gateway-service"
          team    = "dream-team"
          version = var.image_tag
        }
      }

      spec {
        container {
          name              = "gateway-service"
          image             = "${var.docker_registry}/circleguard-gateway-service:${var.image_tag}"
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 8087
          }

          env {
            name  = "SPRING_DATA_REDIS_HOST"
            value = "redis"
          }

          env {
            name  = "SPRING_DATA_REDIS_PORT"
            value = "6379"
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "gateway_service" {
  metadata {
    name      = "gateway-service"
    namespace = var.namespace_name
    labels = {
      app  = "gateway-service"
      team = "dream-team"
    }
  }

  spec {
    type = "NodePort"
    selector = {
      app = "gateway-service"
    }

    port {
      name        = "http"
      protocol    = "TCP"
      port        = 8087
      target_port = 8087
      node_port   = 30087
    }
  }
}

# ============================================================
# 2. Identity Service
# ============================================================

resource "kubernetes_deployment" "identity_service" {
  metadata {
    name      = "identity-service-deployment"
    namespace = var.namespace_name
    labels = {
      app     = "identity-service"
      team    = "dream-team"
      version = var.image_tag
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "identity-service"
      }
    }

    template {
      metadata {
        labels = {
          app     = "identity-service"
          team    = "dream-team"
          version = var.image_tag
        }
      }

      spec {
        container {
          name              = "identity-service"
          image             = "${var.docker_registry}/circleguard-identity-service:${var.image_tag}"
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 8083
          }

          env {
            name  = "SPRING_DATASOURCE_URL"
            value = "jdbc:postgresql://postgres:5432/circleguard_identity"
          }

          env {
            name = "SPRING_DATASOURCE_USERNAME"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "postgres-username"
              }
            }
          }

          env {
            name = "SPRING_DATASOURCE_PASSWORD"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "postgres-password"
              }
            }
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "identity_service" {
  metadata {
    name      = "identity-service"
    namespace = var.namespace_name
    labels = {
      app  = "identity-service"
      team = "dream-team"
    }
  }

  spec {
    type = "ClusterIP"
    selector = {
      app = "identity-service"
    }

    port {
      name        = "http"
      protocol    = "TCP"
      port        = 8083
      target_port = 8083
    }
  }
}

# ============================================================
# 3. Auth Service
# ============================================================

resource "kubernetes_deployment" "auth_service" {
  metadata {
    name      = "auth-service-deployment"
    namespace = var.namespace_name
    labels = {
      app     = "auth-service"
      team    = "dream-team"
      version = var.image_tag
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "auth-service"
      }
    }

    template {
      metadata {
        labels = {
          app     = "auth-service"
          team    = "dream-team"
          version = var.image_tag
        }
      }

      spec {
        container {
          name              = "auth-service"
          image             = "${var.docker_registry}/circleguard-auth-service:${var.image_tag}"
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 8180
          }

          env {
            name  = "SPRING_DATASOURCE_URL"
            value = "jdbc:postgresql://postgres:5432/circleguard_auth"
          }

          env {
            name = "SPRING_DATASOURCE_USERNAME"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "postgres-username"
              }
            }
          }

          env {
            name = "SPRING_DATASOURCE_PASSWORD"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "postgres-password"
              }
            }
          }

          env {
            name  = "SPRING_LDAP_URLS"
            value = "ldap://openldap:389"
          }

          env {
            name  = "IDENTITY_API_URL"
            value = "http://identity-service:8083/api/v1/identities/map"
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "auth_service" {
  metadata {
    name      = "auth-service"
    namespace = var.namespace_name
    labels = {
      app  = "auth-service"
      team = "dream-team"
    }
  }

  spec {
    type = "ClusterIP"
    selector = {
      app = "auth-service"
    }

    port {
      name        = "http"
      protocol    = "TCP"
      port        = 8180
      target_port = 8180
    }
  }
}

# ============================================================
# 4. Form Service
# ============================================================

resource "kubernetes_deployment" "form_service" {
  metadata {
    name      = "form-service-deployment"
    namespace = var.namespace_name
    labels = {
      app     = "form-service"
      team    = "dream-team"
      version = var.image_tag
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "form-service"
      }
    }

    template {
      metadata {
        labels = {
          app     = "form-service"
          team    = "dream-team"
          version = var.image_tag
        }
      }

      spec {
        container {
          name              = "form-service"
          image             = "${var.docker_registry}/circleguard-form-service:${var.image_tag}"
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 8086
          }

          env {
            name  = "SPRING_DATASOURCE_URL"
            value = "jdbc:postgresql://postgres:5432/circleguard_form"
          }

          env {
            name = "SPRING_DATASOURCE_USERNAME"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "postgres-username"
              }
            }
          }

          env {
            name = "SPRING_DATASOURCE_PASSWORD"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "postgres-password"
              }
            }
          }

          env {
            name  = "SPRING_KAFKA_BOOTSTRAP_SERVERS"
            value = "kafka:29092"
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "form_service" {
  metadata {
    name      = "form-service"
    namespace = var.namespace_name
    labels = {
      app  = "form-service"
      team = "dream-team"
    }
  }

  spec {
    type = "ClusterIP"
    selector = {
      app = "form-service"
    }

    port {
      name        = "http"
      protocol    = "TCP"
      port        = 8086
      target_port = 8086
    }
  }
}

# ============================================================
# 5. Notification Service
# ============================================================

resource "kubernetes_deployment" "notification_service" {
  metadata {
    name      = "notification-service-deployment"
    namespace = var.namespace_name
    labels = {
      app     = "notification-service"
      team    = "dream-team"
      version = var.image_tag
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "notification-service"
      }
    }

    template {
      metadata {
        labels = {
          app     = "notification-service"
          team    = "dream-team"
          version = var.image_tag
        }
      }

      spec {
        container {
          name              = "notification-service"
          image             = "${var.docker_registry}/circleguard-notification-service:${var.image_tag}"
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 8082
          }

          env {
            name  = "AUTH_API_URL"
            value = "http://auth-service:8180"
          }

          env {
            name  = "SPRING_KAFKA_BOOTSTRAP_SERVERS"
            value = "kafka:29092"
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "notification_service" {
  metadata {
    name      = "notification-service"
    namespace = var.namespace_name
    labels = {
      app  = "notification-service"
      team = "dream-team"
    }
  }

  spec {
    type = "ClusterIP"
    selector = {
      app = "notification-service"
    }

    port {
      name        = "http"
      protocol    = "TCP"
      port        = 8082
      target_port = 8082
    }
  }
}

# ============================================================
# 6. Promotion Service
# ============================================================

resource "kubernetes_deployment" "promotion_service" {
  metadata {
    name      = "promotion-service-deployment"
    namespace = var.namespace_name
    labels = {
      app     = "promotion-service"
      team    = "dream-team"
      version = var.image_tag
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "promotion-service"
      }
    }

    template {
      metadata {
        labels = {
          app     = "promotion-service"
          team    = "dream-team"
          version = var.image_tag
        }
      }

      spec {
        container {
          name              = "promotion-service"
          image             = "${var.docker_registry}/circleguard-promotion-service:${var.image_tag}"
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 8088
          }

          env {
            name  = "SPRING_DATASOURCE_URL"
            value = "jdbc:postgresql://postgres:5432/circleguard_promotion"
          }

          env {
            name = "SPRING_DATASOURCE_USERNAME"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "postgres-username"
              }
            }
          }

          env {
            name = "SPRING_DATASOURCE_PASSWORD"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "postgres-password"
              }
            }
          }

          env {
            name  = "SPRING_NEO4J_URI"
            value = "bolt://neo4j:7687"
          }

          env {
            name = "SPRING_NEO4J_AUTHENTICATION_USERNAME"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "neo4j-username"
              }
            }
          }

          env {
            name = "SPRING_NEO4J_AUTHENTICATION_PASSWORD"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "neo4j-password"
              }
            }
          }

          env {
            name  = "SPRING_DATA_REDIS_HOST"
            value = "redis"
          }

          env {
            name  = "SPRING_DATA_REDIS_PORT"
            value = "6379"
          }

          env {
            name  = "SPRING_KAFKA_BOOTSTRAP_SERVERS"
            value = "kafka:29092"
          }

          resources {
            requests = {
              memory = "256Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "promotion_service" {
  metadata {
    name      = "promotion-service"
    namespace = var.namespace_name
    labels = {
      app  = "promotion-service"
      team = "dream-team"
    }
  }

  spec {
    type = "ClusterIP"
    selector = {
      app = "promotion-service"
    }

    port {
      name        = "http"
      protocol    = "TCP"
      port        = 8088
      target_port = 8088
    }
  }
}
