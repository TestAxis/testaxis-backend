package io.testaxis.backend.services

import io.testaxis.backend.models.Build
import io.testaxis.backend.models.TestCaseExecution
import io.testaxis.backend.repositories.TestCaseExecutionRepository
import org.springframework.stereotype.Service

@Service
class TestHealthService(val testCaseExecutionRepository: TestCaseExecutionRepository) {
    companion object {
        const val RECENT_BUILDS_AMOUNT = 50
        const val FAILS_OFTEN_THRESHOLD = 0.10
    }

    interface HealthWarning
    data class FailsOftenHealthWarning(val recentFailures: Int) : HealthWarning

    fun investigate(testCaseExecution: TestCaseExecution): List<HealthWarning> {
        val warnings = mutableListOf<HealthWarning>()

        testCaseExecution.countRecentFailures().let { recentFailures ->
            if (recentFailures > FAILS_OFTEN_THRESHOLD * RECENT_BUILDS_AMOUNT) {
                warnings.add(FailsOftenHealthWarning(recentFailures))
            }
        }

        return warnings
    }

    private fun TestCaseExecution.countRecentFailures(): Int {
        val builds = this.build.project.builds

        return builds.sortedByDescending { it.id }.take(RECENT_BUILDS_AMOUNT).count { build ->
            build.testCaseExecutions
                .find {
                    it.testSuiteName == this.testSuiteName && it.name == this.name
                }
                ?.passed?.not() ?: false
        }
    }
}
