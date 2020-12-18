package io.testaxis.backend.http.controllers.api

// Based on https://www.callicoder.com/spring-boot-security-oauth2-social-login-part-1

import io.testaxis.backend.exceptions.ResourceNotFoundException
import io.testaxis.backend.models.User
import io.testaxis.backend.repositories.UserRepository
import io.testaxis.backend.security.CurrentUser
import io.testaxis.backend.security.UserPrincipal
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(val userRepository: UserRepository) {

    @GetMapping("/user/me")
    @PreAuthorize("hasRole('USER')")
    fun show(@CurrentUser userPrincipal: UserPrincipal): User =
        userRepository.findById(userPrincipal.id).orElseThrow { ResourceNotFoundException() }
}
