package io.testaxis.backend.http.controllers

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.multipart

@SpringBootTest
@AutoConfigureMockMvc
class ReportsControllerTest(@Autowired val mockMvc: MockMvc) {
    private val testReport =
        """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="io.testaxis.backend.http.controllers.ProjectControllerTest" tests="2" skipped="0" failures="2" errors="0" timestamp="2020-10-14T13:22:46" time="0.081">
                <properties/>
                <testcase name="A user can retrieve no projects at all()" classname="io.testaxis.backend.http.controllers.ProjectControllerTest" time="0.038">
                    <failure message="java.lang.AssertionError: []: Expected 0 values but got 4" type="java.lang.AssertionError">java.lang.AssertionError: []: Expected 0 values but got 4
                        at org.skyscreamer.jsonassert.JSONAssert.assertEquals(JSONAssert.java:417)
                        (...)
                        at java.base/java.lang.Thread.run(Thread.java:834)
                    </failure>
                </testcase>
                <testcase name="A user can retrieve all projects()" classname="io.testaxis.backend.http.controllers.ProjectControllerTest" time="0.042" />
            </testsuite>
        """.trimIndent()

    @Test
    fun `A user can upload a single XML test report`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
        }.andExpect {
            content { string(containsString("2 tests")) }
        }
    }

    @Test
    fun `A user can upload multiple XML test reports`() {
        mockMvc.multipart("/reports") {
            file(fakeTestReport())
            file(fakeTestReport())
        }.andExpect {
            content { string(containsString("4 tests")) }
        }
    }

    private fun fakeTestReport() = MockMultipartFile(
        "files",
        "test_report.xml",
        MediaType.TEXT_XML_VALUE,
        testReport.byteInputStream()
    )
}
