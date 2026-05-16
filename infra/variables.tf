variable "project_id" {
  type        = string
  description = "GCP project ID hosting all DevTalks resources."
}

variable "region" {
  type        = string
  description = "Default GCP region for all regional resources."
  default     = "us-central1"
}

variable "image_tag" {
  type        = string
  description = "Initial image tag the Cloud Run service is created with. After bootstrap, Cloud Build rolls revisions explicitly via `gcloud run deploy`, and `lifecycle.ignore_changes` keeps Terraform from reverting that."
  default     = "latest"
}

variable "api_gateway_min_instances" {
  type        = number
  description = "Minimum warm instances for api-gateway. Keep at 0 outside of demos to stay in free tier."
  default     = 0
}