package io.testaxis.backend.repositories

import io.testaxis.backend.models.TestCaseExecution
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TestCaseExecutionRepository : CrudRepository<TestCaseExecution, Long>
