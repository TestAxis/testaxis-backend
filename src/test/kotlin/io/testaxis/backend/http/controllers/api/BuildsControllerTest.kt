package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.BaseTest
import io.testaxis.backend.apiRoute
import io.testaxis.backend.models.Build
import io.testaxis.backend.models.BuildStatus
import io.testaxis.backend.models.Project
import io.testaxis.backend.repositories.BuildRepository
import io.testaxis.backend.repositories.ProjectRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import javax.transaction.Transactional

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class BuildsControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val buildRepository: BuildRepository,
    @Autowired val projectRepository: ProjectRepository,
) : BaseTest() {
    @Test
    fun `A user can retrieve all builds for a given project`() {
        val user = fakeUser()
        val project = projectRepository.save(Project(name = "project", slug = "org/project", user = user))
        val builds = buildRepository.saveAll(
            listOf(
                Build(
                    project = project,
                    status = BuildStatus.SUCCESS,
                    branch = "new-feature",
                    commit = "a212a3",
                    slug = "org/project"
                ),
                Build(
                    project = project,
                    status = BuildStatus.TESTS_FAILED,
                    branch = "fix-bug",
                    commit = "b72a73",
                    slug = "org/project"
                ),
            )
        ).toList()
        refresh(user, project)

        mockMvc.get(apiRoute("/projects/${project.id}/builds")) {
            accept = MediaType.APPLICATION_JSON
            asFakeUser(user)
        }.andExpect {
            status { isOk }

            jsonPath("$[0].id") { value(builds[0].id!!) }
            jsonPath("$[0].branch") { value("new-feature") }
            jsonPath("$[0].commit") { value("a212a3") }
            jsonPath("$[0].slug") { value("org/project") }
            jsonPath("$[0].status") { value("success") }

            jsonPath("$[1].id") { value(builds[1].id!!) }
            jsonPath("$[1].branch") { value("fix-bug") }
            jsonPath("$[1].commit") { value("b72a73") }
            jsonPath("$[1].slug") { value("org/project") }
            jsonPath("$[1].status") { value("tests_failed") }
        }
    }

    @Test
    fun `A user can only retrieve builds for a specific project`() {
        val user = fakeUser()
        val validProject = projectRepository.save(Project(name = "project", slug = "org/project", user = user))
        val validProjectBuild = buildRepository.save(
            Build(project = validProject, branch = "new-feature", commit = "a212a3", slug = "org/project")
        )
        val invalidProject = projectRepository.save(Project(name = "project", slug = "org/other-project", user = user))
        buildRepository.save(
            Build(project = invalidProject, branch = "invalid-feature", commit = "a212a3", slug = "org/other-project")
        )
        refresh(user, validProject)

        mockMvc.get(apiRoute("/projects/${validProject.id}/builds")) {
            accept = MediaType.APPLICATION_JSON
            asFakeUser(user)
        }.andExpect {
            status { isOk }

            jsonPath("$[0].id") { value(validProjectBuild.id!!) }
            jsonPath("$[1].id") { doesNotExist() }
        }
    }

    @Test
    fun `A user can only retrieve builds they have access to`() {
        val user = fakeUser()
        val otherUser = fakeUser()
        val validProject = projectRepository.save(Project(name = "project", slug = "org/project", user = otherUser))
        buildRepository.save(
            Build(project = validProject, branch = "new-feature", commit = "a212a3", slug = "org/project")
        )
        refresh(user, validProject)

        mockMvc.get(apiRoute("/projects/${validProject.id}/builds")) {
            accept = MediaType.APPLICATION_JSON
            asFakeUser(user)
        }.andExpect {
            status { isNotFound }
        }
    }
}
