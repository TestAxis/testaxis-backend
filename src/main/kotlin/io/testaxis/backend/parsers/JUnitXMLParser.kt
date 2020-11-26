package io.testaxis.backend.parsers

import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.w3c.dom.Element

@Component
class JUnitXMLParser : XMLParser<JUnitXMLParser.TestSuite>() {

    data class TestSuite(
        val name: String,
        val tests: Int,
        val failures: Int,
        val skipped: Int,
        val errors: Int,
        val time: Double,
        val testCases: List<TestCase>,
    )

    data class TestCase(
        val name: String,
        val className: String,
        val time: Double,
        val passed: Boolean,
        val failureMessage: String?,
        val failureType: String?,
        val failureContent: String?,
    )

    /**
     * Parses one or more XML documents to a collection of [TestSuite]s.
     *
     * In case multiple documents are provided, a flattened collection will be returned where all the testsuites of all
     * the documents are at the same level.
     */
    override fun parseDocuments(documents: List<Document>) =
        documents.map { parseTestSuites(it.documentElement) }.flatten()

    /**
     * Parses the <testsuites> root node.
     *
     * Temporary implementation since the Gradle test XML report does not output this tag.
     */
    private fun parseTestSuites(element: Element): List<TestSuite> = listOf(parseTestSuite(element))

    /**
     * Parses a <testsuite> node into a [TestSuite] object.
     */
    private fun parseTestSuite(element: Element) = TestSuite(
        name = element.getAttribute("name"),
        tests = element.getAttribute("tests").toInt(),
        failures = element.getAttribute("failures").toInt(),
        skipped = element.getAttribute("skipped").toInt(),
        errors = element.getAttribute("errors").toInt(),
        time = element.getAttribute("time").toDouble(),
        testCases = parseTestCases(element.getChildElementsByTagName("testcase"))
    )

    /**
     * Parses a list of <testcase> nodes into a list of [TestCase] objects.
     */
    @Suppress("ForbiddenComment") // TODO: parse <skipped /> and maybe <error ..> ?
    private fun parseTestCases(elements: List<Element>) = elements.map {
        TestCase(
            name = it.getAttribute("name"),
            className = it.getAttribute("classname"),
            time = it.getAttribute("time").toDouble(),
            passed = it.getChildElementsByTagName("failure").isEmpty(),
            failureMessage = it.getChildElementByTagName("failure")?.getAttribute("message"),
            failureType = it.getChildElementByTagName("failure")?.getAttribute("type"),
            failureContent = it.getChildElementByTagName("failure")?.textContent,
        )
    }
}
