package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.exceptions.ResourceNotFoundException
import io.testaxis.backend.http.MustExist
import io.testaxis.backend.http.transformers.BuildTransformer
import io.testaxis.backend.models.Project
import io.testaxis.backend.repositories.UserRepository
import io.testaxis.backend.security.CurrentUser
import io.testaxis.backend.security.UserPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/projects/{project}/builds")
@Validated
class BuildsController(val userRepository: UserRepository, val transformer: BuildTransformer) {
    @GetMapping
    fun index(@CurrentUser userPrincipal: UserPrincipal, @PathVariable @MustExist project: Project) =
        transformer.transform(
            userPrincipal.user(userRepository).projects.find { it == project }?.builds
                ?: throw ResourceNotFoundException(),
            transformer::summary
        )
}
