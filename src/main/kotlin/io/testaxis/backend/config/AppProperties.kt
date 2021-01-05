package io.testaxis.backend.config

// Based on https://www.callicoder.com/spring-boot-security-oauth2-social-login-part-1

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.ArrayList

@ConfigurationProperties(prefix = "app")
class AppProperties {
    val auth = Auth()
    val oauth2 = OAuth2()

    class Auth {
        lateinit var tokenSecret: String
        var tokenExpirationMsec: Long = 0
    }

    class OAuth2 {
        var authorizedRedirectUris: List<String> = ArrayList()
    }
}
