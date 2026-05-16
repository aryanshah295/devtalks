package com.devtalks.apigateway.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// `/api/healthz` (not `/healthz`) because Google's *.run.app edge reserves `/healthz`
// and returns its own 404 before requests reach the container. `/api/healthz` is also
// what the Firebase Hosting rewrite forwards (Hosting preserves the full path).
@RestController
class HealthController {
    @GetMapping("/api/healthz")
    fun healthz(): Map<String, String> = mapOf("status" to "ok")
}