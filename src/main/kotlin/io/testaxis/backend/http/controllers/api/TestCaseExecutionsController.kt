package io.testaxis.backend.http.controllers.api

import io.testaxis.backend.http.MustExist
import io.testaxis.backend.http.transformers.TestCaseExecutionTransformer
import io.testaxis.backend.models.Build
import io.testaxis.backend.models.TestCaseExecution
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
class TestCaseExecutionsController(val transformer: TestCaseExecutionTransformer) {
    @Suppress("ForbiddenComment")
    // TODO: scope build by project
    @GetMapping
    fun index(@PathVariable @MustExist build: Build) =
        transformer.transform(build.testCaseExecutions!!, transformer::summary)

    @Suppress("ForbiddenComment")
    // TODO: scope execution by build and project
    @GetMapping("/{testCaseExecution}")
    fun show(@PathVariable @MustExist testCaseExecution: TestCaseExecution) = transformer.details(testCaseExecution)
}
