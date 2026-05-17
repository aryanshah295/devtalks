# Runtime SA grants. Phase 0 doesn't use most of these, but granting them now
# means the Phase 1+ Firestore + logging + tracing + Secret Manager paths
# work without a separate IAM round-trip later.
locals {
  api_gateway_runtime_roles = [
    "roles/datastore.user",
    "roles/logging.logWriter",
    "roles/cloudtrace.agent",
    "roles/secretmanager.secretAccessor",
  ]
}

resource "google_project_iam_member" "api_gateway_runtime" {
  for_each = toset(local.api_gateway_runtime_roles)
  project  = var.project_id
  role     = each.value
  member   = "serviceAccount:${google_service_account.api_gateway.email}"
}
