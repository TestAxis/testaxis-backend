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
    fun findBySlugOrCreate(slug: String, user: User): Project
}

class CustomProjectRepositoryImpl(val repository: ProjectRepository) : CustomProjectRepository {
    override fun findBySlugOrCreate(slug: String, user: User): Project {
        val project = repository.findBySlug(slug)

        if (project == null || project.user != user) {
            return repository.save(Project(name = Project.splitNameFromSlug(slug), slug = slug, user = user))
        }

        return project
    }
}
