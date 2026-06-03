output "postgres_host" {
  description = "The DNS name of the PostgreSQL service"
  value       = "postgres"
}

output "redis_host" {
  description = "The DNS name of the Redis service"
  value       = "redis"
}

output "neo4j_host" {
  description = "The DNS name of the Neo4j service"
  value       = "neo4j"
}

output "kafka_bootstrap_servers" {
  description = "The bootstrap server address for Kafka"
  value       = "kafka:29092"
}
