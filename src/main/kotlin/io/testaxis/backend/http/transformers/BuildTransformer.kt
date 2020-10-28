package io.testaxis.backend.http.transformers

import io.testaxis.backend.http.transformers.dsl.Transformer
import io.testaxis.backend.models.Build
import org.springframework.stereotype.Component

@Component
class BuildTransformer : Transformer() {
    fun summary(build: Build) = transform(build) {
        "id" - id
        "status" - status.toString().toLowerCase()
        "branch" - branch
        "commit" - commit
        "slug" - slug
        "tag" - tag
        "pr" - pr
        "service" - service
        "service_build" - serviceBuild
        "service_job" - serviceJob
        "created_at" - createdAt
        "updated_at" - createdAt
    }
}
