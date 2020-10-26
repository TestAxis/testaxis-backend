package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.repositories.BuildRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/builds")
class BuildsController(val buildRepository: BuildRepository) {
    @GetMapping
    fun index() = buildRepository.findAll()
}
