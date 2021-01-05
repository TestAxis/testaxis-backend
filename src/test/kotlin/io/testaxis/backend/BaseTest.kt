package io.testaxis.backend

import io.testaxis.backend.models.AuthProvider
import io.testaxis.backend.models.User
import io.testaxis.backend.repositories.UserRepository
import io.testaxis.backend.security.TokenProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import javax.persistence.EntityManager

@SpringBootTest
abstract class BaseTest {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var authenticationManager: AuthenticationManager

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var tokenProvider: TokenProvider

    @Autowired
    lateinit var entityManager: EntityManager

    protected fun fakeUser(
        user: User = User(
            name = "Fake User",
            email = "${randomString()}@user.com",
            provider = AuthProvider.Local
        )
    ): User {
        user.password = passwordEncoder.encode("fake-password")
        return userRepository.save(user)
    }

    protected fun MockHttpServletRequestDsl.asFakeUser(user: User = fakeUser()) {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(user.email, "fake-password")
        )

        SecurityContextHolder.getContext().authentication = authentication

        val token = tokenProvider.createToken(authentication)

        header("Authorization", "Bearer $token")
    }

    protected fun refresh(entity: Any) = entityManager.refresh(entity)
}
