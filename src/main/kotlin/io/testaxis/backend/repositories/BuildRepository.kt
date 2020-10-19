package io.testaxis.backend.repositories

import io.testaxis.backend.models.Build
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BuildRepository : CrudRepository<Build, Long> {
    fun findByCommit(commit: String): Build
}
