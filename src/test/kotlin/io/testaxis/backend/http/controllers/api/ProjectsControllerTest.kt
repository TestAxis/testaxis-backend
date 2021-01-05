package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.BaseTest
import io.testaxis.backend.apiRoute
import io.testaxis.backend.models.Project
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
class ProjectsControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val projectRepository: ProjectRepository
) : BaseTest() {
    @Test
    fun `A user can retrieve all projects`() {
        val user = fakeUser()

        val projects = projectRepository.saveAll(
            listOf(
                Project(name = "example-project-a", slug = "org/example-project-a", user = user),
                Project(name = "example-project-b", slug = "org/example-project-b", user = user)
            )
        ).toList()

        refresh(user)

        mockMvc.get(apiRoute("/projects")) {
            accept = MediaType.APPLICATION_JSON
            asFakeUser(user)
        }.andExpect {
            status { isOk }

            jsonPath("$[0].id") { value(projects[0].id!!) }
            jsonPath("$[0].name") { value("example-project-a") }
            jsonPath("$[0].slug") { value("org/example-project-a") }

            jsonPath("$[1].id") { value(projects[1].id!!) }
            jsonPath("$[1].name") { value("example-project-b") }
            jsonPath("$[1].slug") { value("org/example-project-b") }
        }
    }

    @Test
    fun `A user cannot retrieve project they do not have access to`() {
        val user = fakeUser()
        val otherUser = fakeUser()

        val projects = projectRepository.saveAll(
            listOf(
                Project(name = "example-project-a", slug = "org/example-project-a", user = otherUser),
                Project(name = "example-project-b", slug = "org/example-project-b", user = user)
            )
        ).toList()

        refresh(user)

        mockMvc.get(apiRoute("/projects")) {
            accept = MediaType.APPLICATION_JSON
            asFakeUser(user)
        }.andExpect {
            status { isOk }

            jsonPath("$[0].id") { value(projects[1].id!!) }
            jsonPath("$[0].name") { value("example-project-b") }
            jsonPath("$[0].slug") { value("org/example-project-b") }

            jsonPath("$[1]") { doesNotExist() }
        }
    }

    @Test
    fun `A user can retrieve a single project`() {
        val user = fakeUser()
        val project =
            projectRepository.save(Project(name = "example-project-a", slug = "org/example-project-a", user = user))

        refresh(user)

        mockMvc.get(apiRoute("/projects/${project.id}")) {
            accept = MediaType.APPLICATION_JSON
            asFakeUser(user)
        }.andExpect {
            status { isOk }

            jsonPath("$.id") { value(project.id!!) }
            jsonPath("$.name") { value("example-project-a") }
            jsonPath("$.slug") { value("org/example-project-a") }
        }
    }

    @Test
    fun `A user cannot retrieve a single project they do not have access to`() {
        val user = fakeUser()
        val otherUser = fakeUser()
        val project = projectRepository.save(
            Project(name = "example-project-a", slug = "org/example-project-a", user = otherUser)
        )

        refresh(user)

        mockMvc.get(apiRoute("/projects/${project.id}")) {
            accept = MediaType.APPLICATION_JSON
            asFakeUser(user)
        }.andExpect {
            status { isNotFound }
        }
    }
}
