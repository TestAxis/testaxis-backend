package io.testaxis.backend.services

import io.testaxis.backend.parsers.JUnitXMLParser
import io.testaxis.backend.events.BuildWasCreatedEvent
import io.testaxis.backend.models.Build
import io.testaxis.backend.models.BuildStatus
import io.testaxis.backend.models.TestCaseExecution
import io.testaxis.backend.repositories.BuildRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class ReportService(
    val applicationEventPublisher: ApplicationEventPublisher,
    val buildRepository: BuildRepository,
    val parser: JUnitXMLParser
) {
    fun parseAndPersistTestReports(build: Build, reports: List<InputStream>) =
        buildRepository.save(build).also { persistedBuild ->
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
            }.let { executions ->
                persistedBuild.testCaseExecutions.addAll(executions)
                if (executions.any { !it.passed }) {
                    persistedBuild.status = BuildStatus.TESTS_FAILED
                }
                buildRepository.save(persistedBuild)

                applicationEventPublisher.publishEvent(BuildWasCreatedEvent(this, persistedBuild))
            }
        }
}
