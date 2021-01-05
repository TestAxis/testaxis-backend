package io.testaxis.backend.http.controllers.api

// Based on https://www.callicoder.com/spring-boot-security-oauth2-social-login-part-1

import io.testaxis.backend.exceptions.ResourceNotFoundException
import io.testaxis.backend.http.transformers.UserTransformer
import io.testaxis.backend.http.transformers.dsl.KeyValueData
import io.testaxis.backend.repositories.UserRepository
import io.testaxis.backend.security.CurrentUser
import io.testaxis.backend.security.UserPrincipal
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user")
class UserController(val userRepository: UserRepository, val transformer: UserTransformer) {

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    fun show(@CurrentUser userPrincipal: UserPrincipal): KeyValueData = transformer.details(
        userRepository.findById(userPrincipal.id).orElseThrow { ResourceNotFoundException() }
    )
}
