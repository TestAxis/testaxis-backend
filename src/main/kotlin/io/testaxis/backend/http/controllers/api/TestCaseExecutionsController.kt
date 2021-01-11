package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.exceptions.ResourceNotFoundException
import io.testaxis.backend.http.MustExist
import io.testaxis.backend.http.transformers.TestCaseExecutionTransformer
import io.testaxis.backend.http.transformers.TestHealthWarningTransformer
import io.testaxis.backend.models.Build
import io.testaxis.backend.models.TestCaseExecution
import io.testaxis.backend.repositories.UserRepository
import io.testaxis.backend.security.CurrentUser
import io.testaxis.backend.security.UserPrincipal
import io.testaxis.backend.services.TestHealthService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.transaction.Transactional

@RestController
@Transactional
@RequestMapping("/api/v1/projects/{project}/builds/{build}/testcaseexecutions")
@Validated
class TestCaseExecutionsController(
    val userRepository: UserRepository,
    val testHealthService: TestHealthService,
    val transformer: TestCaseExecutionTransformer,
    val testHealthTransformer: TestHealthWarningTransformer,
) {
    @GetMapping
    fun index(@CurrentUser userPrincipal: UserPrincipal, @PathVariable @MustExist build: Build) =
        transformer.transform(
            userPrincipal.user(userRepository)
                .projects.find { it == build.project }
                ?.builds?.find { it == build }
                ?.testCaseExecutions
                ?: throw ResourceNotFoundException(),
            transformer::summary
        )

    @GetMapping("/{testCaseExecution}")
    fun show(@CurrentUser userPrincipal: UserPrincipal, @PathVariable @MustExist testCaseExecution: TestCaseExecution) =
        transformer.details(
            userPrincipal.user(userRepository)
                .projects.find { it == testCaseExecution.build.project }
                ?.builds?.find { it == testCaseExecution.build }
                ?.testCaseExecutions?.find { it == testCaseExecution }
                ?: throw ResourceNotFoundException()
        )

    @GetMapping("/{testCaseExecution}/health")
    fun showHealth(
        @CurrentUser userPrincipal: UserPrincipal,
        @PathVariable @MustExist testCaseExecution: TestCaseExecution
    ) =
        testHealthTransformer.transform(
            testHealthService.investigate(
                userPrincipal.user(userRepository)
                    .projects.find { it == testCaseExecution.build.project }
                    ?.builds?.find { it == testCaseExecution.build }
                    ?.testCaseExecutions?.find { it == testCaseExecution }
                    ?: throw ResourceNotFoundException()
            ),
            testHealthTransformer::summary
        )
}
