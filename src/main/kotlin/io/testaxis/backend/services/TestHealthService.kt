package io.testaxis.backend.services

import io.testaxis.backend.models.Build
import io.testaxis.backend.models.TestCaseExecution
import org.springframework.stereotype.Service

@Service
class TestHealthService {
    companion object {
        const val RECENT_BUILDS_AMOUNT = 50
        const val FAILS_OFTEN_THRESHOLD = 0.10
    }

    interface HealthWarning
    data class FailsOftenHealthWarning(val recentFailures: Int) : HealthWarning
    data class SlowerThanAverage(val averageTime: Double) : HealthWarning

    fun investigate(testCaseExecution: TestCaseExecution): List<HealthWarning> {
        val warnings = mutableListOf<HealthWarning>()

        testCaseExecution.countRecentFailures().let { recentFailures ->
            if (recentFailures > FAILS_OFTEN_THRESHOLD * RECENT_BUILDS_AMOUNT) {
                warnings.add(FailsOftenHealthWarning(recentFailures))
            }
        }

        testCaseExecution.build.averageTestExecutionTime().let { averageTime ->
            println(averageTime)
            if (testCaseExecution.time > averageTime) {
                warnings.add(SlowerThanAverage(averageTime))
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

    private fun Build.averageTestExecutionTime() = testCaseExecutions.map { it.time }.average()
}
