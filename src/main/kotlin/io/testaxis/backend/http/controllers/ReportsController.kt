package io.testaxis.backend.http.controllers

import io.testaxis.backend.config.AppConfig
import io.testaxis.backend.http.FilesHaveMaxSize
import io.testaxis.backend.http.FilesHaveType
import io.testaxis.backend.models.Build
import io.testaxis.backend.models.BuildStatus
import io.testaxis.backend.models.Project
import io.testaxis.backend.repositories.ProjectRepository
import io.testaxis.backend.services.ReportService
import org.springframework.util.MimeTypeUtils
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@RestController
@Validated
class ReportsController(val reportService: ReportService, val projectRepository: ProjectRepository) {
    @PostMapping("reports")
    fun store(
        @RequestParam("files", required=false)
        @FilesHaveType(types = [MimeTypeUtils.APPLICATION_XML_VALUE, MimeTypeUtils.TEXT_XML_VALUE])
        @FilesHaveMaxSize(size = AppConfig.UPLOAD_LIMIT)
        files: Array<MultipartFile>?,
        @Valid request: UploadReportRequest
    ) = reportService.parseAndPersistTestReports(
        request.toBuild(projectRepository.findBySlugOrCreate(request.slug)),
        files?.map { it.inputStream } ?: emptyList()
    ).let { executions ->
        """
            -------------------------------------------
            TestAxis -- Upload Successful
            -------------------------------------------
            Found the reported executions of ${executions.count()} tests.
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
    @Suppress("ConstructorParameterNaming") val build_status: String?,
) {
    fun toBuild(project: Project) = Build(
        project = project,
        status = BuildStatus.values().find { it.name.toLowerCase() == build_status } ?: BuildStatus.UNKNOWN,
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
