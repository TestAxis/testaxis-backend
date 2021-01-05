package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.BaseTest
import io.testaxis.backend.apiRoute
import io.testaxis.backend.models.AuthProvider
import io.testaxis.backend.models.User
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
class UserControllerTest(
    @Autowired val mockMvc: MockMvc,
) : BaseTest() {
    @Test
    fun `A user can retrieve its details`() {
        val user = User(
            name = "Casper Boone",
            email = "testaxis@casperboone.nl",
            provider = AuthProvider.Local,
            imageUrl = "http://images.com/image.png"
        )

        mockMvc.get(apiRoute("/user/me")) {
            accept = MediaType.APPLICATION_JSON
            asFakeUser(user)
        }.andExpect {
            status { isOk }

            jsonPath("$.id") { value(user.id!!) }
            jsonPath("$.name") { value("Casper Boone") }
            jsonPath("$.email") { value("testaxis@casperboone.nl") }
            jsonPath("$.provider") { value("Local") }
            jsonPath("$.image_url") { value("http://images.com/image.png") }
        }
    }
}
