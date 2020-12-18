package io.testaxis.backend.security

import io.testaxis.backend.Logger
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class TokenAuthenticationFilter(
    private val tokenProvider: TokenProvider,
    private val customUserDetailsService: CustomUserDetailsService
) : OncePerRequestFilter() {

    private val log by Logger()

    @Suppress("TooGenericExceptionCaught")
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val jwt = getJwtTokenFromRequest(request)
            if (tokenProvider.validateToken(jwt)) {
                val userId = tokenProvider.getUserIdFromToken(jwt)
                val userDetails = customUserDetailsService.loadUserById(userId)

                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities).apply {
                        details = WebAuthenticationDetailsSource().buildDetails(request)
                    }
            }
        } catch (exception: Exception) {
            log.error("Could not set user authentication in security context.", exception)
        }
        filterChain.doFilter(request, response)
    }

    private fun getJwtTokenFromRequest(request: HttpServletRequest) =
        request.getHeader("Authorization")?.let {
            if (it.isNotBlank() && it.startsWith("Bearer ")) {
                it.substringAfter("Bearer ")
            } else null
        }
}
