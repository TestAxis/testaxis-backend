package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.exceptions.ResourceNotFoundException
import io.testaxis.backend.http.MustExist
import io.testaxis.backend.http.transformers.ProjectTransformer
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
@RequestMapping("/api/v1/projects")
@Validated
class ProjectsController(val userRepository: UserRepository, val transformer: ProjectTransformer) {
    @GetMapping
    fun index(@CurrentUser userPrincipal: UserPrincipal) =
        transformer.transform(userPrincipal.user(userRepository).projects, transformer::summary)

    @GetMapping("/{project}")
    fun show(@CurrentUser userPrincipal: UserPrincipal, @PathVariable @MustExist project: Project) =
        transformer.details(
            userPrincipal.user(userRepository).projects.find { it == project } ?: throw ResourceNotFoundException()
        )
}
