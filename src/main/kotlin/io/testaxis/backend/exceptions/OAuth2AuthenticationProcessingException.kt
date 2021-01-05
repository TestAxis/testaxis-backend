package io.testaxis.backend.exceptions

import org.springframework.security.core.AuthenticationException

class OAuth2AuthenticationProcessingException(message: String) : AuthenticationException(message)
