package io.testaxis.backend.http.transformers

import io.testaxis.backend.http.transformers.dsl.Transformer
import io.testaxis.backend.models.User
import org.springframework.stereotype.Component

@Component
class UserTransformer(val projectTransformer: ProjectTransformer) : Transformer() {
    fun details(user: User) = transform(user) {
        "id" - id
        "name" - name
        "email" - email
        "image_url" - imageUrl
        "provider" - provider
        "provider_id" - providerId
        "projects" - projectTransformer.transform(projects, projectTransformer::summary)
    }
}
