package io.testaxis.backend.http.controllers

import io.testaxis.backend.actions.ParseJUnitXML
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class ReportsController(val parser: ParseJUnitXML) {
    @PostMapping("reports")
    fun store(@RequestParam("files") files: Array<MultipartFile>): String {
        val testSuites = parser(files.map { it.inputStream })

        return """
            -------------------------------------------
            TestAxis -- Upload Successful
            -------------------------------------------
            Found the reported executions of ${testSuites.sumOf { it.testCases.count() }} tests.
            -------------------------------------------
        """.trimIndent()
    }
}
