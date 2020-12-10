package io.testaxis.backend.parsers

import io.testaxis.backend.parsers.JUnitXMLParser.TestSuite
import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.w3c.dom.Element

@Component
class JacocoXMLParser : XMLParser<JacocoXMLParser.CoverageReportForSingleMethod>() {

    data class CoverageReportForSingleMethod(
        val testClassName: String,
        val testMethodName: String,
        val packages: List<Package>,
    )

    /**
     * Note that classes/counters are not included at the moment. They can be added when necessary.
     */
    data class Package(
        val name: String,
        val sourceFiles: List<SourceFile>
    )

    data class SourceFile(
        val name: String,
        val lines: List<LineStatistics>,
    )

    data class LineStatistics(
        val lineNumber: Int,
        val coveredInstructions: Int,
        val missedInstructions: Int,
        val coveredBranches: Int,
        val missedBranches: Int
    ) {
        fun isCovered() = coveredInstructions = 0
    }

    /**
     * Parses one or more XML documents to a collection of [TestSuite]s.
     *
     * In case multiple documents are provided, a flattened collection will be returned where all the testsuites of all
     * the documents are at the same level.
     */
    override fun parseDocuments(documents: List<Document>) =
        documents.map { parseRootNode(it.documentElement) }

    /**
     * Parses the <report> root node and the <sessioninfo> child node into a [CoverageReportForSingleMethod] object.
     *
     * Temporary implementation since the Gradle test XML report does not output this tag.
     */
    private fun parseRootNode(element: Element): CoverageReportForSingleMethod {
        if (element.getChildElementsByTagName("sessioninfo").size > 1) {
            throw JacocoXMLParserException("Coverage report cannot contain more than one session.")
        }

        val testClassAndName = element.getChildElementByTagName("sessioninfo")?.getAttribute("id")?.split("##")

        if (testClassAndName === null || testClassAndName.size != 2) {
            throw JacocoXMLParserException("Session info ID invalid, should contain the test class and method name.")
        }

        return CoverageReportForSingleMethod(
            testClassName = testClassAndName[0],
            testMethodName = testClassAndName[1],
            packages = parsePackages(element.getChildElementsByTagName("package"))
        )
    }

    /**
     * Parses <package> nodes into [Package] objects.
     */
    private fun parsePackages(elements: List<Element>) = elements.map { element ->
        Package(
            name = element.getAttribute("name"),
            sourceFiles = parseSourceFiles(element.getChildElementsByTagName("sourcefile"))
        )
    }

    /**
     * Parses <sourcefile> nodes into [SourceFile] objects.
     */
    private fun parseSourceFiles(elements: List<Element>) = elements.map { element ->
        SourceFile(
            name = element.getAttribute("name"),
            lines = parseLines(element.getChildElementsByTagName("line"))
        )
    }

    /**
     * Parses <line> nodes into [Line] objects.
     */
    private fun parseLines(elements: List<Element>) = elements.map { element ->
        LineStatistics(
            lineNumber = element.getAttribute("nr").toInt(),
            coveredInstructions = element.getAttribute("ci").toInt(),
            missedInstructions = element.getAttribute("mi").toInt(),
            coveredBranches = element.getAttribute("cb").toInt(),
            missedBranches = element.getAttribute("mb").toInt()
        )
    }

    class JacocoXMLParserException(message: String) : Exception(message)
}
