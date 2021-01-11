package io.testaxis.backend.services

import io.testaxis.backend.BaseTest
import io.testaxis.backend.models.Build
import io.testaxis.backend.models.Project
import io.testaxis.backend.models.TestCaseExecution
import io.testaxis.backend.models.User
import io.testaxis.backend.repositories.BuildRepository
import io.testaxis.backend.repositories.ProjectRepository
import io.testaxis.backend.repositories.TestCaseExecutionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import javax.transaction.Transactional

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class TestHealthServiceTest(
    @Autowired val testHealthService: TestHealthService,
    @Autowired val testCaseExecutionRepository: TestCaseExecutionRepository,
    @Autowired val buildRepository: BuildRepository,
    @Autowired val projectRepository: ProjectRepository,
) : BaseTest() {
    lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = projectRepository.save(Project(name = "project", slug = "org/project", user = fakeUser()))
    }

    fun fakeTestCaseExecution(passed: Boolean = true): TestCaseExecution {
        val build = buildRepository.save(
            Build(
                project = project,
                branch = "new-feature",
                commit = "a212a3",
                slug = "org/project"
            )
        )

        val testCaseExecution = testCaseExecutionRepository.save(
            TestCaseExecution(
                build = build,
                testSuiteName = "io.testaxis.backend.http.controllers.ReportsControllerTest",
                name = "A user can upload a single report with build information that is persisted()",
                className = "io.testaxis.backend.http.controllers.ReportsControllerTest",
                time = 0.043,
                passed = passed,
                failureMessage = null,
                failureType = null,
                failureContent = null,
            )
        )

        refresh(build, project)

        return testCaseExecution
    }

    @Test
    fun `it detects tests that often fail when the amount exceeds the threshold`() {
        repeat(75) { fakeTestCaseExecution(passed = true) }
        repeat(5) { fakeTestCaseExecution(passed = false) }

        val warnings = testHealthService.investigate(fakeTestCaseExecution(passed = false))

        expectThat(warnings).hasSize(1)
        expectThat(warnings[0]).isA<TestHealthService.FailsOftenHealthWarning>().get {
            expectThat(recentFailures).isEqualTo(6)
        }
    }

    @Test
    fun `it does not detect tests that often fail when the amount does not exceed the threshold`() {
        repeat(75) { fakeTestCaseExecution(passed = true) }
        repeat(4) { fakeTestCaseExecution(passed = false) }

        val warnings = testHealthService.investigate(fakeTestCaseExecution(passed = false))

        expectThat(warnings).isEmpty()
    }
}
