package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.http.MustExist
import io.testaxis.backend.http.transformers.ProjectTransformer
import io.testaxis.backend.models.Project
import io.testaxis.backend.repositories.ProjectRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/projects")
class ProjectsController(val projectsRepository: ProjectRepository, val transformer: ProjectTransformer) {
    @Suppress("ForbiddenComment")
    // TODO: scope execution by user
    @GetMapping
    fun index() = transformer.transform(projectsRepository.findAll(), transformer::summary)

    @Suppress("ForbiddenComment")
    // TODO: scope execution by user
    @GetMapping("/{project}")
    fun show(@PathVariable @MustExist project: Project) = transformer.details(project)
}
