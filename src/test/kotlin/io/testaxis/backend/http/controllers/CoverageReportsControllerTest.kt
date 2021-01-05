package io.testaxis.backend.http.controllers

import io.testaxis.backend.BaseTest
import io.testaxis.backend.models.Build
import io.testaxis.backend.models.TestCaseExecution
import io.testaxis.backend.repositories.BuildRepository
import io.testaxis.backend.repositories.ProjectRepository
import io.testaxis.backend.repositories.TestCaseExecutionRepository
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.result.isEqualTo
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsKey
import strikt.assertions.hasEntry
import strikt.assertions.isEmpty
import strikt.assertions.isNotEmpty
import javax.persistence.EntityManager
import javax.transaction.Transactional

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class CoverageReportsControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val buildRepository: BuildRepository,
    @Autowired val projectRepository: ProjectRepository,
    @Autowired val testCaseExecutionRepository: TestCaseExecutionRepository,
) : BaseTest() {
    private val testReport =
        """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
            <report name="example">
                <sessioninfo id="com.example.CalculatorTest##testAddsNumbers" start="1605719602562" dump="1605719602565"/>
                <package name="com/example">
                    <sourcefile name="Counter.java">
                        <line nr="6" mi="2" ci="0" mb="0" cb="0"/>
                        <line nr="7" mi="3" ci="0" mb="0" cb="0"/>
                        <line nr="8" mi="1" ci="0" mb="0" cb="0"/>
                        <line nr="11" mi="3" ci="0" mb="0" cb="0"/>
                        <line nr="15" mi="6" ci="0" mb="0" cb="0"/>
                        <line nr="16" mi="1" ci="0" mb="0" cb="0"/>
                        <line nr="19" mi="6" ci="0" mb="0" cb="0"/>
                        <line nr="20" mi="1" ci="0" mb="0" cb="0"/>
                    </sourcefile>
                    <sourcefile name="Calculator.java">
                        <line nr="6" mi="0" ci="2" mb="0" cb="0"/>
                        <line nr="7" mi="0" ci="3" mb="0" cb="0"/>
                        <line nr="8" mi="0" ci="1" mb="0" cb="0"/>
                        <line nr="11" mi="0" ci="6" mb="0" cb="0"/>
                        <line nr="13" mi="0" ci="3" mb="0" cb="0"/>
                        <line nr="17" mi="6" ci="0" mb="0" cb="0"/>
                        <line nr="19" mi="3" ci="0" mb="0" cb="0"/>
                    </sourcefile>
                </package>
            </report>
        """.trimIndent()

    @Test
    fun `A user can upload a coverage report for an existing test case execution of the given build`() {
        val testCaseExecution = fakeTestCaseExecution()

        mockMvc.multipart("/reports/${testCaseExecution.build.id}/coverage") {
            file(fakeReport())
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
        }

        expectThat(testCaseExecution.coveredLines).isNotEmpty()
    }

    @Test
    fun `A user can upload a coverage report for non existing tests that will be gracefully skipped`() {
        val testCaseExecution = fakeTestCaseExecution(name = "doesNotExist()")

        mockMvc.multipart("/reports/${testCaseExecution.build.id}/coverage") {
            file(fakeReport())
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
        }

        expectThat(testCaseExecution.coveredLines).isEmpty()
    }

    @Test
    fun `A user can upload a coverage report with coverage information where only covered lines are persisted`() {
        val testCaseExecution = fakeTestCaseExecution()

        mockMvc.multipart("/reports/${testCaseExecution.build.id}/coverage") {
            file(fakeReport())
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
        }

        expectThat(testCaseExecution.coveredLines).hasEntry("com/example/Calculator.java", listOf(6, 7, 8, 11, 13))
    }

    @Test
    fun `A user can upload a coverage report where only covered lines are persisted`() {
        val testCaseExecution = fakeTestCaseExecution()

        mockMvc.multipart("/reports/${testCaseExecution.build.id}/coverage") {
            file(fakeReport())
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
        }

        with(testCaseExecution) {
            expectThat(listOf(coveredLines["com/example/Calculator.java"])).not { contains(6, 3) }
            expectThat(coveredLines).not { containsKey("com/example/Counter.java") }
        }
    }

    @Test
    // @Disabled
    fun `A user cannot upload a coverage report with an invalid session id or other parser errors`() {
        val testCaseExecution = fakeTestCaseExecution()

        val report =
            """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
                <report name="example">
                    <sessioninfo id="INVALID" start="1605719602562" dump="1605719602565"/>
                </report>
            """.trimIndent()

        mockMvc.multipart("/reports/${testCaseExecution.build.id}/coverage") {
            file(fakeReport(report = report))
            asFakeUser()
        }.andExpect {
            content {
                status { isEqualTo(422) }
                string(containsString("Failed"))
            }
        }
    }

    private fun fakeTestCaseExecution(name: String = "testAddsNumbers()"): TestCaseExecution {
        val project = projectRepository.findBySlugOrCreate("company/project")
        val build = buildRepository.save(
            Build(branch = "new-feature", commit = "abc123", slug = "company/project", project = project)
        )
        val testCaseExecution = testCaseExecutionRepository.save(
            TestCaseExecution(
                build = build,
                testSuiteName = "com.example.CalculatorTest",
                className = "com.example.CalculatorTest",
                name = name,
                passed = true,
                failureType = null,
                failureMessage = null,
                failureContent = null,
                time = 0.82
            )
        )
        refresh(build)

        return testCaseExecution
    }

    private fun fakeReport(mimeType: String = MediaType.TEXT_XML_VALUE, report: String = testReport) =
        MockMultipartFile(
            "files",
            "test_report.xml",
            mimeType,
            report.byteInputStream()
        )
}
