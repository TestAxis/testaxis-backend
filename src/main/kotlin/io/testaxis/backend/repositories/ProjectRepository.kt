package io.testaxis.backend.repositories

import io.testaxis.backend.models.Project
import io.testaxis.backend.models.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : CrudRepository<Project, Long>, CustomProjectRepository {
    fun findBySlug(slug: String): Project?
}

interface CustomProjectRepository {
    fun findBySlugOrCreate(slug: String): Project
}

class CustomProjectRepositoryImpl(val repository: ProjectRepository) : CustomProjectRepository {
    override fun findBySlugOrCreate(slug: String): Project =
        repository.findBySlug(slug) ?: repository.save(Project(name = Project.splitNameFromSlug(slug), slug = slug, user = User.fake))
}
