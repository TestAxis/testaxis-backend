package io.testaxis.backend.services

import io.testaxis.backend.actions.ParseJUnitXML
import io.testaxis.backend.models.Build
import io.testaxis.backend.models.TestCaseExecution
import io.testaxis.backend.repositories.BuildRepository
import io.testaxis.backend.repositories.TestCaseExecutionRepository
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class ReportService(
    val buildRepository: BuildRepository,
    val testCaseExecutionRepository: TestCaseExecutionRepository,
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
            }.also {
                testCaseExecutionRepository.saveAll(it)
            }
        }
}
