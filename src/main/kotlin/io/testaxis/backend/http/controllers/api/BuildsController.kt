package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.models.Project
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/projects/{project}/builds")
class BuildsController {
    @GetMapping
    fun index(@PathVariable project: Project) = project.builds
}
