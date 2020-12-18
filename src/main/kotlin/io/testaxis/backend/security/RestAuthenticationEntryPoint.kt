package io.testaxis.backend.security

import io.testaxis.backend.Logger
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RestAuthenticationEntryPoint : AuthenticationEntryPoint {
    private val logger by Logger()

    override fun commence(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        exception: AuthenticationException
    ) {
        logger.error("Responding with unauthorized error. Message - {}", exception.message)

        httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.localizedMessage)
    }
}
