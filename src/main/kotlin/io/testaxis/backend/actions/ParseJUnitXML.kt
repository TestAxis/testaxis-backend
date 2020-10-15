package io.testaxis.backend.actions

import io.testaxis.backend.models.TestCase
import io.testaxis.backend.models.TestSuite
import org.springframework.stereotype.Component
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Finds all children that are [Element]s.
 */
fun Element.getChildElementsByTagName(tagName: String) = getElementsByTagName(tagName).run {
    (0..length).map { item(it) }.filterIsInstance<Element>()
}

/**
 * Finds the first child that is a [Element] and returns [null] otherwise.
 */
fun Element.getChildElementByTagName(tagName: String) = getChildElementsByTagName(tagName).firstOrNull()

@Component
class ParseJUnitXML {
    /**
     * Parses one or more XML documents to a collection of [TestSuite]s.
     *
     * In case multiple documents are provided, a flattened collection will be returned where all the testsuites of all
     * the documents are at the same level.
     */
    operator fun invoke(documents: List<InputStream>): List<TestSuite> = documents
        .map { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        .map {
            it.documentElement.normalize()
            parseTestSuites(it.documentElement)
        }
        .flatten()

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
