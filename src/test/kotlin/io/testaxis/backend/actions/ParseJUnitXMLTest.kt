package io.testaxis.backend.actions

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

class ParseJUnitXMLTest {
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
    fun `It parses a single XML test report with a _testsuite_ root element`() {
        val testSuites = ParseJUnitXML()(testReport)

        expectThat(testSuites) hasSize 1
        expectThat(testSuites[0].name) isEqualTo "io.testaxis.backend.http.controllers.ProjectControllerTest"
    }

    @Test
    fun `It parses multiple XML test report with a _testsuite_ root element`() {
        val testSuites = ParseJUnitXML()(testReport, testReport)

        expectThat(testSuites) hasSize 2
        expectThat(testSuites[0].name) isEqualTo "io.testaxis.backend.http.controllers.ProjectControllerTest"
        expectThat(testSuites[1].name) isEqualTo "io.testaxis.backend.http.controllers.ProjectControllerTest"
    }

    @Test
    @Disabled
    fun `It parses a single XML test report with a _testsuites_ root element`() {
    }

    @Test
    @Disabled
    fun `It parses multiple XML test reports with a _testsuites_ root element`() {
    }

    @Test
    fun `It parses a _testsuite_ element`() {
        val testSuites = ParseJUnitXML()(testReport)

        with(testSuites[0]) {
            expectThat(name) isEqualTo "io.testaxis.backend.http.controllers.ProjectControllerTest"
            expectThat(tests) isEqualTo 2
            expectThat(failures) isEqualTo 2
            expectThat(skipped) isEqualTo 0
            expectThat(errors) isEqualTo 0
            expectThat(time) isEqualTo 0.081
        }
    }

    @Test
    fun `It finds and parses the _testcase_ elements`() {
        val testSuites = ParseJUnitXML()(testReport)

        expectThat(testSuites[0].testCases) hasSize 2
    }

    @Test
    fun `It parses failing _testcase_ elements`() {
        val testSuites = ParseJUnitXML()(testReport)

        with(testSuites[0].testCases[0]) {
            expectThat(name) isEqualTo "A user can retrieve no projects at all()"
            expectThat(className) isEqualTo "io.testaxis.backend.http.controllers.ProjectControllerTest"
            expectThat(time) isEqualTo 0.038
            expectThat(passed).isFalse()
            expectThat(failureMessage) isEqualTo "java.lang.AssertionError: []: Expected 0 values but got 4"
            expectThat(failureType) isEqualTo "java.lang.AssertionError"
            expectThat(failureContent).isA<String>()
                .contains("java.lang.AssertionError: []: Expected 0 values but got 4")
                .contains("at org.skyscreamer.jsonassert.JSONAssert.assertEquals(JSONAssert.java:417)")
                .contains("at java.base/java.lang.Thread.run(Thread.java:834)")
        }
    }

    @Test
    fun `It parses passing _testcase_ elements`() {
        val testSuites = ParseJUnitXML()(testReport)

        with(testSuites[0].testCases[1]) {
            expectThat(name) isEqualTo "A user can retrieve all projects()"
            expectThat(className) isEqualTo "io.testaxis.backend.http.controllers.ProjectControllerTest"
            expectThat(time) isEqualTo 0.042
            expectThat(passed).isTrue()
            expectThat(failureMessage).isNull()
            expectThat(failureType).isNull()
            expectThat(failureContent).isNull()
        }
    }
}
