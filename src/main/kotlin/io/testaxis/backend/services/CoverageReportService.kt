package io.testaxis.backend.services

import io.testaxis.backend.Logger
import io.testaxis.backend.ifNull
import io.testaxis.backend.models.Build
import io.testaxis.backend.parsers.JacocoXMLParser
import io.testaxis.backend.repositories.TestCaseExecutionRepository
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class CoverageReportService(
    val testCaseExecutionRepository: TestCaseExecutionRepository,
    val parser: JacocoXMLParser
) {
    private val logger by Logger()

    fun parseAndPersistCoverageReports(build: Build, reports: List<InputStream>) =
        parser(reports).map { report ->
            val results = report.packages
                .flatMap { pkg ->
                    pkg.sourceFiles.map { sourceFile ->
                        if (sourceFile.lines.none { it.isCovered() }) {
                            null
                        } else {
                            val coveredLines = sourceFile.lines.filter { it.isCovered() }.map { it.lineNumber }

                            "${pkg.name}/${sourceFile.name}" to coveredLines
                        }
                    }
                }
                .filterNotNull()
                .toMap()

            build.testCaseExecutions
                .firstOrNull { it.className == report.testClassName && it.name == report.testMethodName }
                .ifNull { logger.warn("No test execution found for ${report.testClassName}.${report.testMethodName}.") }
                ?.apply { coveredLines.putAll(results) }
        }.filterNotNull().also {
            testCaseExecutionRepository.saveAll(it)
        }
}
