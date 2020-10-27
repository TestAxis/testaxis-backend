package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.repositories.ProjectRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/projects")
class ProjectsController(val projectsRepository: ProjectRepository) {
    @GetMapping
    fun index() = projectsRepository.findAll()
}
