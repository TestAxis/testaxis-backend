package io.testaxis.backend.security.oauth2

import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class HttpCookieOAuth2AuthorizationRequestRepository : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? =
        request.getCookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)?.deserialize()

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response)
            return
        }

        response.addSerializedCookie(
            OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
            authorizationRequest,
            cookieExpireSeconds
        )

        val redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME)

        if (redirectUriAfterLogin.isNotBlank()) {
            response.addCookie(REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, cookieExpireSeconds)
        }
    }

    override fun removeAuthorizationRequest(request: HttpServletRequest) = loadAuthorizationRequest(request)

    fun removeAuthorizationRequestCookies(request: HttpServletRequest, response: HttpServletResponse) {
        response.deleteCookie(request.getCookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME))
        response.deleteCookie(request.getCookie(REDIRECT_URI_PARAM_COOKIE_NAME))
    }

    companion object {
        const val OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request"
        const val REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri"
        private const val cookieExpireSeconds = 180
    }
}
