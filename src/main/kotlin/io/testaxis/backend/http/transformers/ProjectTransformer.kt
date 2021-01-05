package io.testaxis.backend.http.transformers

import io.testaxis.backend.http.transformers.dsl.Transformer
import io.testaxis.backend.models.Project
import org.springframework.stereotype.Component

@Component
class ProjectTransformer : Transformer() {
    fun summary(project: Project) = transform(project) {
        "id" - id
        "name" - name
        "slug" - slug
    }

    fun details(project: Project) = transform(project) {
        "id" - id
        "name" - name
        "slug" - slug
    }
}
