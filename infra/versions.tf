terraform {
  required_version = ">= 1.6"

  # The GCS backend does not support variable interpolation in its block,
  # so the bucket name is the one piece of GCP config that lives here as a literal.
  backend "gcs" {
    bucket = "devtalks-aryan-4787-tfstate"
    prefix = "phase0"
  }

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.30"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = "~> 5.30"
    }
  }
}
