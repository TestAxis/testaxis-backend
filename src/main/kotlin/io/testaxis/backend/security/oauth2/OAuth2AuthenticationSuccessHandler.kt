package io.testaxis.backend.security.oauth2

import io.testaxis.backend.Logger
import io.testaxis.backend.config.AppProperties
import io.testaxis.backend.exceptions.BadRequestException
import io.testaxis.backend.security.TokenProvider
import io.testaxis.backend.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.Companion.REDIRECT_URI_PARAM_COOKIE_NAME
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class OAuth2AuthenticationSuccessHandler(
    val tokenProvider: TokenProvider,
    val appProperties: AppProperties,
    val httpCookieOAuth2AuthorizationRequestRepository: HttpCookieOAuth2AuthorizationRequestRepository
) : SimpleUrlAuthenticationSuccessHandler() {

    private val log by Logger()

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val targetUrl = determineTargetUrl(request, response, authentication)

        if (response.isCommitted) {
            log.debug("Response has already been committed. Unable to redirect to $targetUrl.")
            return
        }

        clearAuthenticationAttributes(request, response)

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }

    override fun determineTargetUrl(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ): String {
        val redirectUri = request.getCookie(REDIRECT_URI_PARAM_COOKIE_NAME)?.value

        if (redirectUri != null && !isAuthorizedRedirectUri(redirectUri)) {
            throw BadRequestException("Sorry! The redirect uri is not authorized.")
        }

        return UriComponentsBuilder
            .fromUriString(redirectUri ?: defaultTargetUrl)
            .queryParam("token", tokenProvider.createToken(authentication))
            .build()
            .toUriString()
    }

    private fun clearAuthenticationAttributes(request: HttpServletRequest, response: HttpServletResponse) {
        super.clearAuthenticationAttributes(request)

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response)
    }

    /**
     * Only validate host and port. Let the clients use different paths if they want to.
     */
    private fun isAuthorizedRedirectUri(uri: String): Boolean {
        val clientRedirectUri = URI.create(uri)

        return appProperties.oauth2.authorizedRedirectUris.any { authorizedRedirectUri ->
            val authorizedURI = URI.create(authorizedRedirectUri)

            authorizedURI.host.equals(clientRedirectUri.host, ignoreCase = true) &&
                authorizedURI.port == clientRedirectUri.port
        }
    }
}
