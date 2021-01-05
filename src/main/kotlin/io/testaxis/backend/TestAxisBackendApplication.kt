package io.testaxis.backend

import io.testaxis.backend.config.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class TestAxisBackendApplication

fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<TestAxisBackendApplication>(*args)
}
