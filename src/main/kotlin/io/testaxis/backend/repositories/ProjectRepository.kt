package io.testaxis.backend.repositories

import io.testaxis.backend.models.Project
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : CrudRepository<Project, Long>
