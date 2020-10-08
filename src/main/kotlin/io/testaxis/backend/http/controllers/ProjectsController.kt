package io.testaxis.backend.http.controllers

import io.testaxis.backend.repositories.ProjectRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ProjectsController(val projectsRepository: ProjectRepository) {
    @GetMapping("/projects")
    fun index() = projectsRepository.findAll()
}
