package io.testaxis.backend.http.controllers

import io.testaxis.backend.config.AppConfig
import io.testaxis.backend.http.FilesHaveMaxSize
import io.testaxis.backend.http.FilesHaveType
import io.testaxis.backend.models.Build
import io.testaxis.backend.services.ReportService
import org.springframework.util.MimeTypeUtils
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty

@RestController
@Validated
class ReportsController(val reportService: ReportService) {
    @PostMapping("reports")
    fun store(
        @RequestParam("files") @NotEmpty
        @FilesHaveType(types = [MimeTypeUtils.APPLICATION_XML_VALUE, MimeTypeUtils.TEXT_XML_VALUE])
        @FilesHaveMaxSize(size = AppConfig.UPLOAD_LIMIT)
        files: Array<MultipartFile>,
        @Valid request: UploadReportRequest
    ) = reportService.parseAndPersistTestReports(
        request.toBuild(),
        files.map { it.inputStream }
    ).let { executions ->
        """
            -------------------------------------------
            TestAxis -- Upload Successful
            -------------------------------------------
            Found the reported executions of ${executions.count()} tests.
            $request
            -------------------------------------------
        """.trimIndent()
    }
}

data class UploadReportRequest(
    @field:NotBlank val branch: String = "",
    @field:NotBlank val commit: String = "",
    @field:NotBlank val slug: String = "",
    val tag: String?,
    val pr: String?,
    val service: String?,
    val build: String?,
    @Suppress("ConstructorParameterNaming") val build_url: String?,
    val job: String?,
) {
    fun toBuild() = Build(
        branch = branch,
        commit = commit,
        slug = slug,
        tag = tag,
        pr = pr,
        service = service,
        serviceBuild = build,
        serviceBuildUrl = build_url,
        serviceJob = job,
    )
}
