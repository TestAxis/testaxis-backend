package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.BaseTest
import io.testaxis.backend.apiRoute
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
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import javax.persistence.EntityManager
import javax.transaction.Transactional

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class TestCaseExecutionsControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val testCaseExecutionRepository: TestCaseExecutionRepository,
    @Autowired val buildRepository: BuildRepository,
    @Autowired val projectRepository: ProjectRepository,
) : BaseTest() {
    lateinit var user: User
    lateinit var project: Project
    lateinit var build: Build
    lateinit var testCaseExecutions: List<TestCaseExecution>

    @BeforeEach
    fun setUp() {
        user = fakeUser()
        project = projectRepository.save(Project(name = "project", slug = "org/project", user = user))
        build = buildRepository.save(Build(project = project, branch = "new-feature", commit = "a212a3", slug = "org/project"))
        testCaseExecutions = testCaseExecutionRepository.saveAll(
            listOf(
                TestCaseExecution(
                    build = build,
                    testSuiteName = "io.testaxis.backend.http.controllers.ReportsControllerTest",
                    name = "A user can upload a single report with build information that is persisted()",
                    className = "io.testaxis.backend.http.controllers.ReportsControllerTest",
                    time = 0.043,
                    passed = true,
                    failureMessage = null,
                    failureType = null,
                    failureContent = null,
                ),
                TestCaseExecution(
                    build = build,
                    testSuiteName = "io.testaxis.backend.http.controllers.ReportsControllerTest",
                    name = "A user can upload multiple reports with build information that is persisted()",
                    className = "io.testaxis.backend.http.controllers.ReportsControllerTest",
                    time = 0.043,
                    passed = false,
                    failureMessage = "An error occurred",
                    failureType = "org.opentest4j.AssertionFailedError",
                    failureContent = "The stacktrace of the failure",
                )
            )
        ).toList()
        refresh(build)
    }

    @Test
    fun `A user can retrieve all test case executions for a given build`() {
        mockMvc.get(apiRoute("/projects/${project.id}/builds/${build.id}/testcaseexecutions")) {
            accept = MediaType.APPLICATION_JSON
            asFakeUser()
        }.andExpect {
            status { isOk }

            jsonPath("$[0].id") { value(testCaseExecutions[0].id!!) }
            jsonPath("$[0].name") { value("A user can upload a single report with build information that is persisted()") }
            jsonPath("$[0].class_name") { value("io.testaxis.backend.http.controllers.ReportsControllerTest") }
            jsonPath("$[0].passed") { value(true) }
            jsonPath("$[0].failure_message") { doesNotExist() }

            jsonPath("$[1].id") { value(testCaseExecutions[1].id!!) }
            jsonPath("$[1].name") { value("A user can upload multiple reports with build information that is persisted()") }
            jsonPath("$[1].class_name") { value("io.testaxis.backend.http.controllers.ReportsControllerTest") }
            jsonPath("$[1].passed") { value(false) }
            jsonPath("$[1].failure_message") { doesNotExist() }
        }
    }

    @Test
    fun `A user can retrieve a successful single test case execution with more details`() {
        mockMvc.get(apiRoute("/projects/${project.id}/builds/${build.id}/testcaseexecutions/${testCaseExecutions[0].id}")) {
            accept = MediaType.APPLICATION_JSON
            asFakeUser()
        }.andExpect {
            status { isOk }

            jsonPath("$.id") { value(testCaseExecutions[0].id!!) }
            jsonPath("$.name") { value("A user can upload a single report with build information that is persisted()") }
            jsonPath("$.class_name") { value("io.testaxis.backend.http.controllers.ReportsControllerTest") }
            jsonPath("$.passed") { value(true) }
            jsonPath("$.failure_message") { isEmpty }
        }
    }

    @Test
    fun `A user can retrieve a failing single test case execution with more details`() {
        mockMvc.get(apiRoute("/projects/${project.id}/builds/${build.id}/testcaseexecutions/${testCaseExecutions[1].id}")) {
            accept = MediaType.APPLICATION_JSON
            asFakeUser()
        }.andExpect {
            status { isOk }

            jsonPath("$.id") { value(testCaseExecutions[1].id!!) }
            jsonPath("$.name") { value("A user can upload multiple reports with build information that is persisted()") }
            jsonPath("$.class_name") { value("io.testaxis.backend.http.controllers.ReportsControllerTest") }
            jsonPath("$.passed") { value(false) }
            jsonPath("$.failure_message") { value("An error occurred") }
            jsonPath("$.failure_content") { value("The stacktrace of the failure") }
        }
    }
}
