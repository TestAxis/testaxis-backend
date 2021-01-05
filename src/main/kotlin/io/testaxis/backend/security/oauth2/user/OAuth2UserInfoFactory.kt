package io.testaxis.backend.security.oauth2.user

import io.testaxis.backend.exceptions.OAuth2AuthenticationProcessingException
import io.testaxis.backend.models.AuthProvider

object OAuth2UserInfoFactory {
    fun getOAuth2UserInfo(registrationId: String, attributes: Map<String, Any>): OAuth2UserInfo {
        return if (registrationId.equals(AuthProvider.GitHub.toString(), ignoreCase = true)) {
            GithubOAuth2UserInfo(attributes)
        } else {
            throw OAuth2AuthenticationProcessingException("Sorry! Login with $registrationId is not supported yet.")
        }
    }
}
