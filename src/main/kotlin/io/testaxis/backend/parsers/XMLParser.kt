package io.testaxis.backend.parsers

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Finds all children that are [Element]s.
 */
internal fun Element.getChildElementsByTagName(tagName: String) = getElementsByTagName(tagName).run {
    (0..length).map { item(it) }.filterIsInstance<Element>()
}

/**
 * Finds the first child that is a [Element] and returns null otherwise.
 */
internal fun Element.getChildElementByTagName(tagName: String) = getChildElementsByTagName(tagName).firstOrNull()

abstract class XMLParser<R> {
    /**
     * Parses one or more XML documents to a collection of [R]s.
     */
    operator fun invoke(documents: List<InputStream>): List<R> = documents
        .map { createDocumentBuilder().parse(it) }
        .onEach { it.documentElement.normalize() }
        .let { parseDocuments(it) }

    private fun createDocumentBuilder() = DocumentBuilderFactory.newInstance()
        .apply {
            isValidating = false
            isNamespaceAware = true;
            setFeature("http://xml.org/sax/features/namespaces", false);
            setFeature("http://xml.org/sax/features/validation", false);
            setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        }
        .newDocumentBuilder()

    /**
     * Parses all provided XML documents.
     */
    protected abstract fun parseDocuments(documents: List<Document>): List<R>
}
