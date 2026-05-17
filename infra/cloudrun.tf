resource "google_service_account" "api_gateway" {
  account_id   = "sa-api-gateway"
  display_name = "Runtime SA for the api-gateway Cloud Run service"
}

resource "google_cloud_run_v2_service" "api_gateway" {
  name     = "api-gateway"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.api_gateway.email

    scaling {
      min_instance_count = var.api_gateway_min_instances
      max_instance_count = 5
    }

    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/devtalks/api-gateway:${var.image_tag}"

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
        cpu_idle          = true
        startup_cpu_boost = true
      }

      env {
        name  = "GCP_PROJECT_ID"
        value = var.project_id
      }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }

      startup_probe {
        http_get {
          path = "/actuator/health/liveness"
          port = 8080
        }
        initial_delay_seconds = 5
        timeout_seconds       = 3
        period_seconds        = 10
        failure_threshold     = 12
      }

      liveness_probe {
        http_get {
          path = "/actuator/health/liveness"
          port = 8080
        }
        period_seconds = 30
      }
    }
  }

  # Cloud Build rolls images via `gcloud run deploy --image=...`. Terraform should not
  # revert the running revision to the image_tag baked into this file on every apply.
  lifecycle {
    ignore_changes = [template[0].containers[0].image]
  }
}

resource "google_cloud_run_v2_service_iam_member" "api_gateway_public" {
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.api_gateway.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
