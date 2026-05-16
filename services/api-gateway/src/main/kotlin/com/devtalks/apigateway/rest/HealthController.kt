package com.devtalks.apigateway.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// Serve both paths so:
//   - /healthz answers when curling the raw Cloud Run URL (matches PLAN.md verification)
//   - /api/healthz answers via the Firebase Hosting rewrite, which preserves the full path
@RestController
class HealthController {
    @GetMapping(value = ["/healthz", "/api/healthz"])
    fun healthz(): Map<String, String> = mapOf("status" to "ok")
}