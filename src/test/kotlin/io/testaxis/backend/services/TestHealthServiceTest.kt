package io.testaxis.backend.services

import io.testaxis.backend.BaseTest
import io.testaxis.backend.models.Build
import io.testaxis.backend.models.Project
import io.testaxis.backend.models.TestCaseExecution
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

    fun fakeTestCaseExecution(passed: Boolean = true, time: Double = 0.043, build: Build? = null): TestCaseExecution {
        val persistedBuild = build ?: buildRepository.save(
            Build(project = project, branch = "new-feature", commit = "a212a3", slug = "org/project")
        )

        val testCaseExecution = testCaseExecutionRepository.save(
            TestCaseExecution(
                build = persistedBuild,
                testSuiteName = "io.testaxis.backend.http.controllers.ReportsControllerTest",
                name = "A user can upload a single report with build information that is persisted()",
                className = "io.testaxis.backend.http.controllers.ReportsControllerTest",
                time = time,
                passed = passed,
                failureMessage = null,
                failureType = null,
                failureContent = null,
            )
        )

        refresh(persistedBuild, project)

        return testCaseExecution
    }

    @Test
    fun `it detects tests that often fail when the amount exceeds the threshold`() {
        repeat(75) { fakeTestCaseExecution(passed = true) }
        repeat(5) { fakeTestCaseExecution(passed = false) }

        val warnings = testHealthService.investigate(fakeTestCaseExecution(passed = false))

        expectThat(warnings).hasSize(1)
        expectThat(warnings[0]).isA<TestHealthService.HealthWarning<Int>>().get {
            expectThat(value).isEqualTo(6)
        }
    }

    @Test
    fun `it does not detect tests that often fail when the amount does not exceed the threshold`() {
        repeat(75) { fakeTestCaseExecution(passed = true) }
        repeat(4) { fakeTestCaseExecution(passed = false) }

        val warnings = testHealthService.investigate(fakeTestCaseExecution(passed = false))

        expectThat(warnings).isEmpty()
    }

    @Test
    fun `it detects tests that are slower than average`() {
        val build = buildRepository.save(
            Build(project = project, branch = "new-feature", commit = "a212a3", slug = "org/project")
        )

        fakeTestCaseExecution(time = 0.1, build = build)
        fakeTestCaseExecution(time = 0.2, build = build)
        fakeTestCaseExecution(time = 0.4, build = build)

        val warnings = testHealthService.investigate(fakeTestCaseExecution(time = 0.3, build = build))

        expectThat(warnings).hasSize(1)
        expectThat(warnings[0]).isA<TestHealthService.HealthWarning<Double>>().get {
            expectThat(value).isEqualTo(0.25)
        }
    }

    @Test
    fun `it does not detect tests that are not slower than average`() {
        val build = buildRepository.save(
            Build(project = project, branch = "new-feature", commit = "a212a3", slug = "org/project")
        )

        fakeTestCaseExecution(time = 0.1, build = build)
        fakeTestCaseExecution(time = 0.3, build = build)
        fakeTestCaseExecution(time = 0.4, build = build)

        val warnings = testHealthService.investigate(fakeTestCaseExecution(time = 0.2, build = build))

        expectThat(warnings).isEmpty()
    }
}
