package io.testaxis.backend.http.controllers

import io.testaxis.backend.jsonContent
import io.testaxis.backend.models.Project
import io.testaxis.backend.repositories.ProjectRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerTest(@Autowired val mockMvc: MockMvc, @Autowired val projectRepository: ProjectRepository) {
    @Test
    fun `A user can retrieve all projects`() {
        val projects = projectRepository.saveAll(
            listOf(
                Project(name = "example-project-a"),
                Project(name = "example-project-b")
            )
        )

        mockMvc.get("/projects") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk }
            content { jsonContent(projects) }
        }
    }
}
