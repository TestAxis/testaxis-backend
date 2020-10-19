package io.testaxis.backend.http.controllers

import io.testaxis.backend.services.ReportService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class ReportsController(val reportService: ReportService) {
    @PostMapping("reports")
    fun store(@RequestParam("files") files: Array<MultipartFile>): String {
        val testCaseExecutions = reportService.parseAndPersistTestReports(files.map { it.inputStream })

        return """
            -------------------------------------------
            TestAxis -- Upload Successful
            -------------------------------------------
            Found the reported executions of ${testCaseExecutions.count()} tests.
            -------------------------------------------
        """.trimIndent()
    }
}
