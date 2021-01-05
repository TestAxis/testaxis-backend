package io.testaxis.backend.http.controllers

import io.testaxis.backend.BaseTest
import io.testaxis.backend.hasValidationError
import io.testaxis.backend.http.transformers.dsl.KeyValueData
import io.testaxis.backend.models.BuildStatus
import io.testaxis.backend.repositories.BuildRepository
import io.testaxis.backend.repositories.ProjectRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.result.isEqualTo
import org.springframework.util.MimeTypeUtils
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import javax.persistence.EntityManager
import javax.transaction.Transactional

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class ReportsControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val buildRepository: BuildRepository,
    @Autowired val projectRepository: ProjectRepository,
) : BaseTest() {
    @SpyBean
    lateinit var simpMessagingTemplate: SimpMessagingTemplate

    private val testReport =
        """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="io.testaxis.backend.http.controllers.ProjectControllerTest" tests="2" skipped="0" failures="2" errors="0" timestamp="2020-10-14T13:22:46" time="0.081">
                <properties/>
                <testcase name="A user can retrieve no projects at all()" classname="io.testaxis.backend.http.controllers.ProjectControllerTest" time="0.038">
                    <failure message="java.lang.AssertionError: []: Expected 0 values but got 4" type="java.lang.AssertionError">java.lang.AssertionError: []: Expected 0 values but got 4
                        at org.skyscreamer.jsonassert.JSONAssert.assertEquals(JSONAssert.java:417)
                        (...)
                        at java.base/java.lang.Thread.run(Thread.java:834)
                    </failure>
                </testcase>
                <testcase name="A user can retrieve all projects()" classname="io.testaxis.backend.http.controllers.ProjectControllerTest" time="0.042" />
            </testsuite>
        """.trimIndent()

    @Test
    fun `A user can upload a report for a new project which is then created`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            param("commit", "abc123")
            param("branch", "new-feature")
            param("slug", "company/project")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
        }

        val createdProject = projectRepository.findBySlug("company/project")
        expectThat(createdProject).isNotNull()
        with(buildRepository.findByCommit("abc123")) {
            refresh(this)

            expectThat(project).equals(createdProject!!)
        }
    }

    @Test
    fun `A user can upload a report for an existing project which does not get re-created`() {
        val fakeProject = projectRepository.findBySlugOrCreate("company/project")

        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            param("commit", "abc123")
            param("branch", "new-feature")
            param("slug", "company/project")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
        }

        expectThat(projectRepository.count()).isEqualTo(1)
        with(buildRepository.findByCommit("abc123")) {
            refresh(this)

            expectThat(project).equals(fakeProject)
        }
    }

    @Test
    fun `A user can upload a single report of which the content of failing tests is persisted`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            param("commit", "abc123")
            param("branch", "new-feature")
            param("slug", "company/project")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
        }

        with(buildRepository.findByCommit("abc123")) {
            refresh(this) // TODO: Check if this refresh method is the correct approach

            expectThat(testCaseExecutions) hasSize 2

            expectThat(testCaseExecutions[0].name) isEqualTo "A user can retrieve no projects at all()"
            expectThat(testCaseExecutions[0].className) isEqualTo "io.testaxis.backend.http.controllers.ProjectControllerTest"
            expectThat(testCaseExecutions[0].time) isEqualTo 0.038
            expectThat(testCaseExecutions[0].passed).isFalse()
            expectThat(testCaseExecutions[0].failureMessage) isEqualTo "java.lang.AssertionError: []: Expected 0 values but got 4"
            expectThat(testCaseExecutions[0].failureType) isEqualTo "java.lang.AssertionError"
            expectThat(testCaseExecutions[0].failureContent).isA<String>()
                .contains("java.lang.AssertionError: []: Expected 0 values but got 4")
                .contains("at org.skyscreamer.jsonassert.JSONAssert.assertEquals(JSONAssert.java:417)")
                .contains("at java.base/java.lang.Thread.run(Thread.java:834)")
        }
    }

    @Test
    fun `A user can upload a single report of which the content of passing tests is (also) persisted`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            param("commit", "abc123")
            param("branch", "new-feature")
            param("slug", "company/project")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
        }

        with(buildRepository.findByCommit("abc123")) {
            refresh(this) // TODO: Check if this refresh method is the correct approach

            expectThat(testCaseExecutions) hasSize 2

            expectThat(testCaseExecutions[1].name) isEqualTo "A user can retrieve all projects()"
            expectThat(testCaseExecutions[1].className) isEqualTo "io.testaxis.backend.http.controllers.ProjectControllerTest"
            expectThat(testCaseExecutions[1].time) isEqualTo 0.042
            expectThat(testCaseExecutions[1].passed).isTrue()
            expectThat(testCaseExecutions[1].failureMessage).isNull()
            expectThat(testCaseExecutions[1].failureType).isNull()
            expectThat(testCaseExecutions[1].failureContent).isNull()
        }
    }

    @Test
    fun `A user can upload a single report with build information that is persisted`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            param("commit", "ae12be2")
            param("branch", "new-feature")
            param("slug", "company/project")
            param("tag", "v1.0.1")
            param("pr", "62")
            param("service", "ci-runner-inc")
            param("build", "2343")
            param("build_url", "http://ci-runner.com/run/2343")
            param("job", "1234")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
        }

        with(buildRepository.findByCommit("ae12be2")) {
            expectThat(commit) isEqualTo "ae12be2"
            expectThat(branch) isEqualTo "new-feature"
            expectThat(slug) isEqualTo "company/project"
            expectThat(tag) isEqualTo "v1.0.1"
            expectThat(pr) isEqualTo "62"
            expectThat(service) isEqualTo "ci-runner-inc"
            expectThat(serviceBuild) isEqualTo "2343"
            expectThat(serviceBuildUrl) isEqualTo "http://ci-runner.com/run/2343"
            expectThat(serviceJob) isEqualTo "1234"
        }
    }

    @Test
    fun `A user can upload a single XML test report of type text--xml`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            param("commit", "a2b4fa")
            param("branch", "new-feature")
            param("slug", "company/project")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
            content { string(containsString("2 tests")) }
        }
    }

    @Test
    fun `A user can upload a single XML test report of type application--xml`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport(MediaType.APPLICATION_XML_VALUE))
            param("commit", "a2b4fa")
            param("branch", "new-feature")
            param("slug", "company/project")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
            content { string(containsString("2 tests")) }
        }
    }

    @Test
    fun `A user can upload multiple XML test reports`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            file(fakeTestReport())
            param("commit", "a2b4fa")
            param("branch", "new-feature")
            param("slug", "company/project")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
            content { string(containsString("4 tests")) }
        }
    }

    @Test
    fun `A user cannot upload an XML test report of an unsupported type`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport(MimeTypeUtils.IMAGE_PNG_VALUE))
            param("commit", "a2b4fa")
            param("branch", "new-feature")
            param("slug", "company/project")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(422) }
            content { hasValidationError("store.files") }
        }
    }

    @Test
    fun `A user cannot upload a report without a commit hash`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            param("branch", "new-feature")
            param("slug", "company/project")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(422) }
            content { hasValidationError("commit") }
        }
    }

    @Test
    fun `A user cannot upload a report without a branch`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            param("commit", "a2b4fa")
            param("slug", "company/project")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(422) }
            content { hasValidationError("branch") }
        }
    }

    @Test
    fun `A user cannot upload a report without a slug`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            param("commit", "a2b4fa")
            param("branch", "new-feature")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(422) }
            content { hasValidationError("slug") }
        }
    }

    @Test
    fun `A user is notified through websockets after uploading a test report`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            param("commit", "a2b4fa")
            param("branch", "new-feature")
            param("slug", "company/project")
            asFakeUser()
        }

        verify(simpMessagingTemplate).convertAndSend(eq("/topic/builds"), any<KeyValueData>())
    }

    @Test
    fun `A user can report a build status without a test report`() {
        mockMvc.multipart("/reports") {
            param("build_status", "success")
            param("commit", "ae12be2")
            param("branch", "new-feature")
            param("slug", "company/project")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
        }

        with(buildRepository.findByCommit("ae12be2")) {
            expectThat(commit) isEqualTo "ae12be2"
            expectThat(branch) isEqualTo "new-feature"
            expectThat(slug) isEqualTo "company/project"
            expectThat(status) isEqualTo BuildStatus.SUCCESS
        }
    }

    @Test
    fun `A user can upload a test report and a build status that gets overridden because tests failed`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            param("build_status", "build_failed")
            param("commit", "ae12be2")
            param("branch", "new-feature")
            param("slug", "company/project")
            asFakeUser()
        }.andExpect {
            status { isEqualTo(200) }
        }

        with(buildRepository.findByCommit("ae12be2")) {
            expectThat(commit) isEqualTo "ae12be2"
            expectThat(branch) isEqualTo "new-feature"
            expectThat(slug) isEqualTo "company/project"
            expectThat(status) isEqualTo BuildStatus.TESTS_FAILED
        }
    }

    private fun fakeTestReport(mimeType: String = MediaType.TEXT_XML_VALUE) = MockMultipartFile(
        "files",
        "test_report.xml",
        mimeType,
        testReport.byteInputStream()
    )
}
