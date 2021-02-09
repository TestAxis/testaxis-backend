package io.testaxis.backend.http.controllers

import io.testaxis.backend.events.BuildWasCreatedEvent
import io.testaxis.backend.http.MustExist
import io.testaxis.backend.models.Build
import io.testaxis.backend.repositories.BuildRepository
import io.testaxis.backend.repositories.ProjectRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.Date

@RestController
class ExperimentController(
    val projectRepository: ProjectRepository,
    val buildRepository: BuildRepository,
    val applicationEventPublisher: ApplicationEventPublisher
) {
    /**
     * Hides all builds that are not on the main branch of the JPacman project.
     */
    @GetMapping("/experiment/hide-all")
    fun hideAllBuilds() {
        projectRepository.findBySlug("testaxis/jpacman")
            ?.builds
            ?.filter { it.branch != "main" }
            ?.forEach {
                it.visible = false
                buildRepository.save(it)
            }
    }

    /**
     * Show all builds of the JPacman project.
     */
    @GetMapping("/experiment/show-all")
    fun showAllBuilds() {
        projectRepository.findBySlug("testaxis/jpacman")
            ?.builds
            ?.forEach {
                it.visible = true
                buildRepository.save(it)
            }
    }

    /**
     * Shows a given build and publishes a new event to announce it.
     */
    @GetMapping("/experiment/show/{build}")
    fun showBuild(@PathVariable @MustExist build: Build) {
        build.visible = true
        build.createdAt = Date()
        val persistedBuild = buildRepository.save(build)

        applicationEventPublisher.publishEvent(BuildWasCreatedEvent(this, persistedBuild))
    }
}
