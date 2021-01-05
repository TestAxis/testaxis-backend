package io.testaxis.backend.http.controllers

import io.testaxis.backend.config.AppConfig
import io.testaxis.backend.http.FilesHaveMaxSize
import io.testaxis.backend.http.FilesHaveType
import io.testaxis.backend.http.MustExist
import io.testaxis.backend.models.Build
import io.testaxis.backend.parsers.JacocoXMLParser
import io.testaxis.backend.services.CoverageReportService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.MimeTypeUtils
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.lang.reflect.UndeclaredThrowableException

@RestController
@Validated
class CoverageReportsController(
    val coverageReportService: CoverageReportService
) {
    @PostMapping("reports/{build}/coverage")
    fun store(
        @PathVariable @MustExist build: Build,
        @RequestParam("files", required = false)
        @FilesHaveType(types = [MimeTypeUtils.APPLICATION_XML_VALUE, MimeTypeUtils.TEXT_XML_VALUE])
        @FilesHaveMaxSize(size = AppConfig.UPLOAD_LIMIT)
        files: Array<MultipartFile>?,
    ) =
        try {
            coverageReportService.parseAndPersistCoverageReports(build, files?.map { it.inputStream } ?: emptyList())
                .let { tests ->
                    ResponseEntity(
                        tests.filter { it.coveredLines.isNotEmpty() }.count().let { coveredTests ->
                            """
                                -------------------------------------------
                                TestAxis -- Coverage Upload
                                -------------------------------------------
                                ${files?.count() ?: 0} files uploaded.
                                Found the reported coverage of $coveredTests tests.
                                -------------------------------------------
                                Build
                                ${build.id}
                            """.trimIndent()
                        },
                        HttpStatus.OK
                    )
                }
        } catch (exception: UndeclaredThrowableException) {
            when (exception.undeclaredThrowable) {
                is JacocoXMLParser.JacocoXMLParserException -> ResponseEntity(
                    """
                        -------------------------------------------
                        TestAxis -- Coverage Upload Failed
                        -------------------------------------------
                        ${exception.message}
                        -------------------------------------------
                    """.trimIndent(),
                    HttpStatus.UNPROCESSABLE_ENTITY
                )
                else -> throw exception
            }
        }
}
