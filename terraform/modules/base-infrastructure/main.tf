# ============================================================
# 1. PostgreSQL Infrastructure
# ============================================================

resource "kubernetes_config_map" "postgres_init_config" {
  metadata {
    name      = "postgres-init-config"
    namespace = var.namespace_name
    labels = {
      app  = "postgres"
      team = "dream-team"
    }
  }

  data = {
    "init-db.sql" = <<-EOT
      -- Initialize required microservice databases
      SELECT 'CREATE DATABASE circleguard_auth'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'circleguard_auth')\gexec
      SELECT 'CREATE DATABASE circleguard_identity'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'circleguard_identity')\gexec
      SELECT 'CREATE DATABASE circleguard_promotion'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'circleguard_promotion')\gexec
      SELECT 'CREATE DATABASE circleguard_dashboard'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'circleguard_dashboard')\gexec
      SELECT 'CREATE DATABASE circleguard_form'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'circleguard_form')\gexec
    EOT
  }
}

resource "kubernetes_persistent_volume_claim" "postgres_pvc" {
  metadata {
    name      = "postgres-pvc"
    namespace = var.namespace_name
    labels = {
      app  = "postgres"
      team = "dream-team"
    }
  }

  spec {
    access_modes = ["ReadWriteOnce"]
    resources {
      requests = {
        storage = "2Gi"
      }
    }
  }
}

resource "kubernetes_deployment" "postgres" {
  metadata {
    name      = "postgres-deployment"
    namespace = var.namespace_name
    labels = {
      app  = "postgres"
      team = "dream-team"
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "postgres"
      }
    }

    template {
      metadata {
        labels = {
          app  = "postgres"
          team = "dream-team"
        }
      }

      spec {
        container {
          name              = "postgres"
          image             = "postgres:16"
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 5432
          }

          env {
            name = "POSTGRES_USER"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "postgres-username"
              }
            }
          }

          env {
            name = "POSTGRES_PASSWORD"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "postgres-password"
              }
            }
          }

          env {
            name  = "POSTGRES_DB"
            value = "circleguard"
          }

          volume_mount {
            name       = "postgres-data"
            mount_path = /var/lib/postgresql/data
          }

          volume_mount {
            name       = "init-script"
            mount_path = "/docker-entrypoint-initdb.d/init-db.sql"
            sub_path   = "init-db.sql"
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

          readiness_probe {
            exec {
              command = ["pg_isready", "-U", "admin", "-d", "circleguard"]
            }
            initial_delay_seconds = 10
            period_seconds        = 5
            failure_threshold     = 6
          }

          liveness_probe {
            exec {
              command = ["pg_isready", "-U", "admin", "-d", "circleguard"]
            }
            initial_delay_seconds = 30
            period_seconds        = 20
            failure_threshold     = 3
          }
        }

        volume {
          name = "postgres-data"
          persistent_volume_claim {
            claim_name = kubernetes_persistent_volume_claim.postgres_pvc.metadata[0].name
          }
        }

        volume {
          name = "init-script"
          config_map {
            name = kubernetes_config_map.postgres_init_config.metadata[0].name
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "postgres" {
  metadata {
    name      = "postgres"
    namespace = var.namespace_name
    labels = {
      app  = "postgres"
      team = "dream-team"
    }
  }

  spec {
    type = "ClusterIP"
    selector = {
      app = "postgres"
    }

    port {
      name        = "postgres"
      protocol    = "TCP"
      port        = 5432
      target_port = 5432
    }
  }
}

# ============================================================
# 2. Redis Infrastructure
# ============================================================

resource "kubernetes_deployment" "redis" {
  metadata {
    name      = "redis-deployment"
    namespace = var.namespace_name
    labels = {
      app  = "redis"
      team = "dream-team"
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "redis"
      }
    }

    template {
      metadata {
        labels = {
          app  = "redis"
          team = "dream-team"
        }
      }

      spec {
        container {
          name              = "redis"
          image             = "redis:7.2"
          image_pull_policy = "IfNotPresent"
          command           = ["redis-server", "--save", "", "--appendonly", "no"]

          port {
            container_port = 6379
          }

          resources {
            requests = {
              memory = "64Mi"
              cpu    = "100m"
            }
            limits = {
              memory = "256Mi"
              cpu    = "250m"
            }
          }

          readiness_probe {
            exec {
              command = ["redis-cli", "ping"]
            }
            initial_delay_seconds = 5
            period_seconds        = 5
            failure_threshold     = 3
          }

          liveness_probe {
            exec {
              command = ["redis-cli", "ping"]
            }
            initial_delay_seconds = 15
            period_seconds        = 20
            failure_threshold     = 3
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "redis" {
  metadata {
    name      = "redis"
    namespace = var.namespace_name
    labels = {
      app  = "redis"
      team = "dream-team"
    }
  }

  spec {
    type = "ClusterIP"
    selector = {
      app = "redis"
    }

    port {
      name        = "redis"
      protocol    = "TCP"
      port        = 6379
      target_port = 6379
    }
  }
}

# ============================================================
# 3. Neo4j Infrastructure
# ============================================================

resource "kubernetes_persistent_volume_claim" "neo4j_pvc" {
  metadata {
    name      = "neo4j-pvc"
    namespace = var.namespace_name
    labels = {
      app  = "neo4j"
      team = "dream-team"
    }
  }

  spec {
    access_modes = ["ReadWriteOnce"]
    resources {
      requests = {
        storage = "2Gi"
      }
    }
  }
}

resource "kubernetes_deployment" "neo4j" {
  metadata {
    name      = "neo4j-deployment"
    namespace = var.namespace_name
    labels = {
      app  = "neo4j"
      team = "dream-team"
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "neo4j"
      }
    }

    template {
      metadata {
        labels = {
          app  = "neo4j"
          team = "dream-team"
        }
      }

      spec {
        enable_service_links = false

        container {
          name              = "neo4j"
          image             = "neo4j:5.26"
          image_pull_policy = "IfNotPresent"

          port {
            name           = "http"
            container_port = 7474
          }

          port {
            name           = "bolt"
            container_port = 7687
          }

          env {
            name = "NEO4J_AUTH"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "neo4j-auth"
              }
            }
          }

          env {
            name  = "NEO4J_PLUGINS"
            value = "[\"apoc\"]"
          }

          env {
            name  = "NEO4J_server_allow__upgrade"
            value = "true"
          }

          env {
            name  = "NEO4J_server_config_strict__validation_enabled"
            value = "false"
          }

          volume_mount {
            name       = "neo4j-data"
            mount_path = "/data"
          }

          resources {
            requests = {
              memory = "1Gi"
              cpu    = "250m"
            }
            limits = {
              memory = "2Gi"
              cpu    = "500m"
            }
          }

          readiness_probe {
            http_get {
              path = "/"
              port = 7474
            }
            initial_delay_seconds = 30
            period_seconds        = 10
            failure_threshold     = 6
          }

          liveness_probe {
            http_get {
              path = "/"
              port = 7474
            }
            initial_delay_seconds = 60
            period_seconds        = 20
            failure_threshold     = 3
          }
        }

        volume {
          name = "neo4j-data"
          persistent_volume_claim {
            claim_name = kubernetes_persistent_volume_claim.neo4j_pvc.metadata[0].name
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "neo4j" {
  metadata {
    name      = "neo4j"
    namespace = var.namespace_name
    labels = {
      app  = "neo4j"
      team = "dream-team"
    }
  }

  spec {
    type = "ClusterIP"
    selector = {
      app = "neo4j"
    }

    port {
      name        = "http"
      protocol    = "TCP"
      port        = 7474
      target_port = 7474
    }

    port {
      name        = "bolt"
      protocol    = "TCP"
      port        = 7687
      target_port = 7687
    }
  }
}

# ============================================================
# 4. OpenLDAP Infrastructure
# ============================================================

resource "kubernetes_deployment" "openldap" {
  metadata {
    name      = "openldap-deployment"
    namespace = var.namespace_name
    labels = {
      app  = "openldap"
      team = "dream-team"
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "openldap"
      }
    }

    template {
      metadata {
        labels = {
          app  = "openldap"
          team = "dream-team"
        }
      }

      spec {
        container {
          name              = "openldap"
          image             = "osixia/openldap:1.5.0"
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 389
            name           = "ldap"
          }

          port {
            container_port = 636
            name           = "ldaps"
          }

          env {
            name  = "LDAP_ORGANISATION"
            value = "CircleGuard"
          }

          env {
            name  = "LDAP_DOMAIN"
            value = "circleguard.edu"
          }

          env {
            name = "LDAP_ADMIN_PASSWORD"
            value_from {
              secret_key_ref {
                name = "circleguard-secrets"
                key  = "ldap-admin-password"
              }
            }
          }

          resources {
            requests = {
              memory = "64Mi"
              cpu    = "100m"
            }
            limits = {
              memory = "256Mi"
              cpu    = "250m"
            }
          }

          readiness_probe {
            tcp_socket {
              port = 389
            }
            initial_delay_seconds = 15
            period_seconds        = 10
            failure_threshold     = 3
          }

          liveness_probe {
            tcp_socket {
              port = 389
            }
            initial_delay_seconds = 30
            period_seconds        = 20
            failure_threshold     = 3
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "openldap" {
  metadata {
    name      = "openldap"
    namespace = var.namespace_name
    labels = {
      app  = "openldap"
      team = "dream-team"
    }
  }

  spec {
    type = "ClusterIP"
    selector = {
      app = "openldap"
    }

    port {
      name        = "ldap"
      protocol    = "TCP"
      port        = 389
      target_port = 389
    }

    port {
      name        = "ldaps"
      protocol    = "TCP"
      port        = 636
      target_port = 636
    }
  }
}

# ============================================================
# 5. Zookeeper Infrastructure
# ============================================================

resource "kubernetes_deployment" "zookeeper" {
  metadata {
    name      = "zookeeper-deployment"
    namespace = var.namespace_name
    labels = {
      app  = "zookeeper"
      team = "dream-team"
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "zookeeper"
      }
    }

    template {
      metadata {
        labels = {
          app  = "zookeeper"
          team = "dream-team"
        }
      }

      spec {
        container {
          name              = "zookeeper"
          image             = "confluentinc/cp-zookeeper:7.6.0"
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 2181
          }

          env {
            name  = "ZOOKEEPER_CLIENT_PORT"
            value = "2181"
          }

          env {
            name  = "ZOOKEEPER_TICK_TIME"
            value = "2000"
          }

          resources {
            requests = {
              memory = "128Mi"
              cpu    = "100m"
            }
            limits = {
              memory = "256Mi"
              cpu    = "250m"
            }
          }

          readiness_probe {
            tcp_socket {
              port = 2181
            }
            initial_delay_seconds = 10
            period_seconds        = 5
            failure_threshold     = 6
          }

          liveness_probe {
            tcp_socket {
              port = 2181
            }
            initial_delay_seconds = 20
            period_seconds        = 15
            failure_threshold     = 3
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "zookeeper" {
  metadata {
    name      = "zookeeper"
    namespace = var.namespace_name
    labels = {
      app  = "zookeeper"
      team = "dream-team"
    }
  }

  spec {
    type = "ClusterIP"
    selector = {
      app = "zookeeper"
    }

    port {
      name        = "client"
      protocol    = "TCP"
      port        = 2181
      target_port = 2181
    }
  }
}

# ============================================================
# 6. Kafka Infrastructure
# ============================================================

resource "kubernetes_deployment" "kafka" {
  metadata {
    name      = "kafka-deployment"
    namespace = var.namespace_name
    labels = {
      app  = "kafka"
      team = "dream-team"
    }
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "kafka"
      }
    }

    template {
      metadata {
        labels = {
          app  = "kafka"
          team = "dream-team"
        }
      }

      spec {
        enable_service_links = false

        container {
          name              = "kafka"
          image             = "confluentinc/cp-kafka:7.6.0"
          image_pull_policy = "IfNotPresent"

          port {
            container_port = 29092
            name           = "internal"
          }

          env {
            name  = "KAFKA_BROKER_ID"
            value = "1"
          }

          env {
            name  = "KAFKA_ZOOKEEPER_CONNECT"
            value = "zookeeper:2181"
          }

          env {
            name  = "KAFKA_ADVERTISED_LISTENERS"
            value = "PLAINTEXT://kafka:29092"
          }

          env {
            name  = "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP"
            value = "PLAINTEXT:PLAINTEXT"
          }

          env {
            name  = "KAFKA_INTER_BROKER_LISTENER_NAME"
            value = "PLAINTEXT"
          }

          env {
            name  = "KAFKA_LISTENERS"
            value = "PLAINTEXT://0.0.0.0:29092"
          }

          env {
            name  = "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR"
            value = "1"
          }

          env {
            name  = "KAFKA_AUTO_CREATE_TOPICS_ENABLE"
            value = "true"
          }

          resources {
            requests = {
              memory = "1Gi"
              cpu    = "250m"
            }
            limits = {
              memory = "2Gi"
              cpu    = "500m"
            }
          }

          readiness_probe {
            tcp_socket {
              port = 29092
            }
            initial_delay_seconds = 20
            period_seconds        = 10
            failure_threshold     = 6
          }

          liveness_probe {
            tcp_socket {
              port = 29092
            }
            initial_delay_seconds = 40
            period_seconds        = 20
            failure_threshold     = 3
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "kafka" {
  metadata {
    name      = "kafka"
    namespace = var.namespace_name
    labels = {
      app  = "kafka"
      team = "dream-team"
    }
  }

  spec {
    type = "ClusterIP"
    selector = {
      app = "kafka"
    }

    port {
      name        = "internal"
      protocol    = "TCP"
      port        = 29092
      target_port = 29092
    }
  }
}
