package io.testaxis.backend.security.oauth2

import io.testaxis.backend.exceptions.OAuth2AuthenticationProcessingException
import io.testaxis.backend.models.AuthProvider
import io.testaxis.backend.models.User
import io.testaxis.backend.repositories.UserRepository
import io.testaxis.backend.security.UserPrincipal
import io.testaxis.backend.security.oauth2.user.GithubOAuth2UserInfo
import io.testaxis.backend.security.oauth2.user.OAuth2UserInfo
import io.testaxis.backend.security.oauth2.user.OAuth2UserInfoFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.web.client.exchange

@Service
class CustomOAuth2UserService(
    val userRepository: UserRepository,
    val restTemplateBuilder: RestTemplateBuilder
) : DefaultOAuth2UserService() {

    @Suppress("TooGenericExceptionCaught")
    override fun loadUser(oAuth2UserRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(oAuth2UserRequest)

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User)
        } catch (exception: AuthenticationException) {
            throw exception
        } catch (exception: Exception) {
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw InternalAuthenticationServiceException(exception.message, exception.cause)
        }
    }

    private fun processOAuth2User(oAuth2UserRequest: OAuth2UserRequest, oAuth2User: OAuth2User): OAuth2User {
        var oAuth2UserInfo: OAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
            oAuth2UserRequest.clientRegistration.registrationId,
            oAuth2User.attributes
        )

        if (matchAuthProvider(oAuth2UserRequest.clientRegistration.registrationId) == AuthProvider.GitHub) {
            oAuth2UserInfo = GithubOAuth2UserInfo(
                oAuth2UserInfo.attributes + ("email" to retrieveGitHubEmail(oAuth2UserRequest.accessToken))
            )
        }

        val email = oAuth2UserInfo.email
        if (email == null || email.isEmpty()) {
            throw OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider")
        }

        val existingUser = userRepository.findByEmail(email)
        val user = if (existingUser != null) {
            checkIfUserUsesCorrectProvider(existingUser, oAuth2UserRequest)
            updateExistingUser(existingUser, oAuth2UserInfo)
        } else {
            registerNewUser(oAuth2UserRequest, oAuth2UserInfo)
        }

        return UserPrincipal.create(user, oAuth2User.attributes)
    }

    private fun checkIfUserUsesCorrectProvider(user: User, oAuth2UserRequest: OAuth2UserRequest) {
        if (user.provider != matchAuthProvider(oAuth2UserRequest.clientRegistration.registrationId)) {
            throw OAuth2AuthenticationProcessingException(
                "Looks like you're signed up with a ${user.provider} account. " +
                    "Please use your ${user.provider} account to login."
            )
        }
    }

    private fun registerNewUser(oAuth2UserRequest: OAuth2UserRequest, oAuth2UserInfo: OAuth2UserInfo) =
        userRepository.save(
            User(
                name = oAuth2UserInfo.name ?: error("Name not present in user info."),
                email = oAuth2UserInfo.email ?: error("Name not present in user info."),
                provider = matchAuthProvider(oAuth2UserRequest.clientRegistration.registrationId),
                providerId = oAuth2UserInfo.id,
                imageUrl = oAuth2UserInfo.imageUrl
            )
        )

    private fun updateExistingUser(existingUser: User, oAuth2UserInfo: OAuth2UserInfo): User {
        existingUser.name = oAuth2UserInfo.name ?: error("Name not present in user info.")
        existingUser.imageUrl = oAuth2UserInfo.imageUrl

        return userRepository.save(existingUser)
    }

    private fun retrieveGitHubEmail(accessToken: OAuth2AccessToken): String {
        val headers = HttpHeaders().apply {
            accept = listOf(MediaType.APPLICATION_JSON)
            setBearerAuth(accessToken.tokenValue)
        }

        val httpEntity = HttpEntity<List<Map<String, Any>>>(headers)

        val entity = restTemplateBuilder.build().exchange<List<Map<String, Any>>>(
            "https://api.github.com/user/emails",
            HttpMethod.GET,
            httpEntity
        )

        return entity.body?.find { it["primary"] == true }?.get("email")?.toString()
            ?: throw OAuth2AuthenticationProcessingException("Email not provided by GitHub.")
    }
}

fun matchAuthProvider(value: String) = AuthProvider.values().find { it.name.toLowerCase() == value }
    ?: error("Provider [$value] not supported.")
