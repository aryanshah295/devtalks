output "api_gateway_url" {
  description = "Public HTTPS URL for the api-gateway Cloud Run service."
  value       = google_cloud_run_v2_service.api_gateway.uri
}

output "api_gateway_service_account_email" {
  description = "Runtime service account email used by api-gateway."
  value       = google_service_account.api_gateway.email
}
