package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.apiRoute
import io.testaxis.backend.models.Build
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
import javax.persistence.EntityManager
import javax.transaction.Transactional

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class BuildsControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val buildRepository: BuildRepository,
    @Autowired val projectRepository: ProjectRepository,
    @Autowired val entityManager: EntityManager
) {
    @Test
    fun `A user can retrieve all builds for a given project`() {
        val project = projectRepository.save(Project(name = "project", slug = "org/project"))
        val builds = buildRepository.saveAll(
            listOf(
                Build(project = project, branch = "new-feature", commit = "a212a3", slug = "org/project"),
                Build(project = project, branch = "fix-bug", commit = "b72a73", slug = "org/project"),
            )
        ).toList()
        entityManager.refresh(project)

        mockMvc.get(apiRoute("/projects/${project.id}/builds")) {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk }

            jsonPath("$[0].id") { value(builds[0].id!!) }
            jsonPath("$[0].branch") { value("new-feature") }
            jsonPath("$[0].commit") { value("a212a3") }
            jsonPath("$[0].slug") { value("org/project") }

            jsonPath("$[1].id") { value(builds[1].id!!) }
            jsonPath("$[1].branch") { value("fix-bug") }
            jsonPath("$[1].commit") { value("b72a73") }
            jsonPath("$[1].slug") { value("org/project") }
        }
    }

    @Test
    fun `A user can only retrieve builds for a specific project`() {
        val validProject = projectRepository.save(Project(name = "project", slug = "org/project"))
        val validProjectBuild = buildRepository.save(
            Build(project = validProject, branch = "new-feature", commit = "a212a3", slug = "org/project")
        )
        val invalidProject = projectRepository.save(Project(name = "project", slug = "org/other-project"))
        buildRepository.save(
            Build(project = invalidProject, branch = "invalid-feature", commit = "a212a3", slug = "org/other-project")
        )
        entityManager.refresh(validProject)

        mockMvc.get(apiRoute("/projects/${validProject.id}/builds")) {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk }

            jsonPath("$[0].id") { value(validProjectBuild.id!!) }
            jsonPath("$[1].id") { doesNotExist() }
        }
    }
}
