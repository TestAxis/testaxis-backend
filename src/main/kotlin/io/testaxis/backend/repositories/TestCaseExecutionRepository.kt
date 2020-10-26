package io.testaxis.backend.repositories

import io.testaxis.backend.models.Build
import io.testaxis.backend.models.TestCaseExecution
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
@Transactional
interface TestCaseExecutionRepository : CrudRepository<TestCaseExecution, Long> {
    fun findByBuild(build: Build): List<TestCaseExecution>
}
