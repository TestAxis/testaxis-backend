package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.apiRoute
import io.testaxis.backend.models.Build
import io.testaxis.backend.repositories.BuildRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class BuildsControllerTest(@Autowired val mockMvc: MockMvc, @Autowired val buildRepository: BuildRepository) {
    @Test
    fun `A user can retrieve all builds for a given project`() {
        val builds = buildRepository.saveAll(
            listOf(
                Build(branch = "new-feature", commit = "a212a3", slug = "org/project"),
                Build(branch = "fix-bug", commit = "b72a73", slug = "org/other-project"),
            )
        ).toList()

        mockMvc.get(apiRoute("/builds")) {
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
            jsonPath("$[1].slug") { value("org/other-project") }
        }
    }
}
