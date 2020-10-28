package io.testaxis.backend.services

import io.testaxis.backend.actions.ParseJUnitXML
import io.testaxis.backend.models.Build
import io.testaxis.backend.models.BuildStatus
import io.testaxis.backend.models.TestCaseExecution
import io.testaxis.backend.repositories.BuildRepository
import io.testaxis.backend.repositories.ProjectRepository
import io.testaxis.backend.repositories.TestCaseExecutionRepository
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class ReportService(
    val buildRepository: BuildRepository,
    val parser: ParseJUnitXML
) {
    fun parseAndPersistTestReports(build: Build, reports: List<InputStream>) =
        buildRepository.save(build).let { persistedBuild ->
            parser(reports).flatMap { testSuite ->
                testSuite.testCases.map {
                    TestCaseExecution(
                        build = persistedBuild,
                        testSuiteName = testSuite.name,
                        name = it.name,
                        className = it.className,
                        time = it.time,
                        passed = it.passed,
                        failureMessage = it.failureMessage,
                        failureType = it.failureType,
                        failureContent = it.failureContent,
                    )
                }
            }.also { executions ->
                build.testCaseExecutions.addAll(executions)
                build.status = if (executions.all { it.passed }) BuildStatus.SUCCESS else BuildStatus.TESTS_FAILED
                buildRepository.save(build)
            }
        }
}
