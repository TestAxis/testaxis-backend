package io.testaxis.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TestAxisBackendApplication

fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<TestAxisBackendApplication>(*args)
}
