package io.testaxis.backend.http.transformers

import io.testaxis.backend.http.transformers.dsl.Transformer
import io.testaxis.backend.services.TestHealthService
import org.springframework.stereotype.Component

@Component
class TestHealthWarningTransformer : Transformer() {
    fun summary(healthWarning: TestHealthService.HealthWarning<*>) = transform(healthWarning) {
        "type" - type.toString().toLowerCase()
        "value" - value
    }
}
