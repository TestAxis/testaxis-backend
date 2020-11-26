package io.testaxis.backend.http.transformers

import io.testaxis.backend.http.transformers.dsl.Transformer
import io.testaxis.backend.models.TestCaseExecution
import org.springframework.stereotype.Component

@Component
class TestCaseExecutionTransformer : Transformer() {
    fun summary(testCaseExecution: TestCaseExecution) = transform(testCaseExecution) {
        "id" - id
        "build_id" - build.id
        "test_suite_name" - testSuiteName
        "name" - name
        "class_name" - className
        "time" - time
        "passed" - passed
        "created_at" - createdAt
    }

    fun details(testCaseExecution: TestCaseExecution) = transform(testCaseExecution, ::summary) {
        "failure_message" - failureMessage
        "failure_type" - failureType
        "failure_content" - failureContent
        "covered_lines" - coveredLines
    }
}
