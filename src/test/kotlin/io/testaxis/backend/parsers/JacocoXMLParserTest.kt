package io.testaxis.backend.parsers

import io.testaxis.backend.parsers.JacocoXMLParser.LineStatistics
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

class JacocoXMLParserTest {
    private val testReport =
        """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
            <report name="example">
                <sessioninfo id="com.example.CalculatorTest##testAddsNumbers" start="1605719602562" dump="1605719602565"/>
                <package name="com/example">
                    <class name="com/example/Calculator" sourcefilename="Calculator.java">
                        <method name="&lt;init&gt;" desc="(I)V" line="6">
                            <counter type="INSTRUCTION" missed="0" covered="6"/>
                            <counter type="LINE" missed="0" covered="3"/>
                            <counter type="COMPLEXITY" missed="0" covered="1"/>
                            <counter type="METHOD" missed="0" covered="1"/>
                        </method>
                        <method name="add" desc="(I)I" line="11">
                            <counter type="INSTRUCTION" missed="0" covered="9"/>
                            <counter type="LINE" missed="0" covered="2"/>
                            <counter type="COMPLEXITY" missed="0" covered="1"/>
                            <counter type="METHOD" missed="0" covered="1"/>
                        </method>
                        <method name="subtract" desc="(I)I" line="17">
                            <counter type="INSTRUCTION" missed="9" covered="0"/>
                            <counter type="LINE" missed="2" covered="0"/>
                            <counter type="COMPLEXITY" missed="1" covered="0"/>
                            <counter type="METHOD" missed="1" covered="0"/>
                        </method>
                        <counter type="INSTRUCTION" missed="9" covered="15"/>
                        <counter type="LINE" missed="2" covered="5"/>
                        <counter type="COMPLEXITY" missed="1" covered="2"/>
                        <counter type="METHOD" missed="1" covered="2"/>
                        <counter type="CLASS" missed="0" covered="1"/>
                    </class>
                    <class name="com/example/Counter" sourcefilename="Counter.java">
                        <method name="&lt;init&gt;" desc="(I)V" line="6">
                            <counter type="INSTRUCTION" missed="6" covered="0"/>
                            <counter type="LINE" missed="3" covered="0"/>
                            <counter type="COMPLEXITY" missed="1" covered="0"/>
                            <counter type="METHOD" missed="1" covered="0"/>
                        </method>
                        <method name="getCount" desc="()I" line="11">
                            <counter type="INSTRUCTION" missed="3" covered="0"/>
                            <counter type="LINE" missed="1" covered="0"/>
                            <counter type="COMPLEXITY" missed="1" covered="0"/>
                            <counter type="METHOD" missed="1" covered="0"/>
                        </method>
                        <method name="increase" desc="()V" line="15">
                            <counter type="INSTRUCTION" missed="7" covered="0"/>
                            <counter type="LINE" missed="2" covered="0"/>
                            <counter type="COMPLEXITY" missed="1" covered="0"/>
                            <counter type="METHOD" missed="1" covered="0"/>
                        </method>
                        <method name="decrease" desc="()V" line="19">
                            <counter type="INSTRUCTION" missed="7" covered="0"/>
                            <counter type="LINE" missed="2" covered="0"/>
                            <counter type="COMPLEXITY" missed="1" covered="0"/>
                            <counter type="METHOD" missed="1" covered="0"/>
                        </method>
                        <counter type="INSTRUCTION" missed="23" covered="0"/>
                        <counter type="LINE" missed="8" covered="0"/>
                        <counter type="COMPLEXITY" missed="4" covered="0"/>
                        <counter type="METHOD" missed="4" covered="0"/>
                        <counter type="CLASS" missed="1" covered="0"/>
                    </class>
                    <sourcefile name="Counter.java">
                        <line nr="6" mi="2" ci="0" mb="0" cb="0"/>
                        <line nr="7" mi="3" ci="0" mb="0" cb="0"/>
                        <line nr="8" mi="1" ci="0" mb="0" cb="0"/>
                        <line nr="11" mi="3" ci="0" mb="0" cb="0"/>
                        <line nr="15" mi="6" ci="0" mb="0" cb="0"/>
                        <line nr="16" mi="1" ci="0" mb="0" cb="0"/>
                        <line nr="19" mi="6" ci="0" mb="0" cb="0"/>
                        <line nr="20" mi="1" ci="0" mb="0" cb="0"/>
                        <counter type="INSTRUCTION" missed="23" covered="0"/>
                        <counter type="LINE" missed="8" covered="0"/>
                        <counter type="COMPLEXITY" missed="4" covered="0"/>
                        <counter type="METHOD" missed="4" covered="0"/>
                        <counter type="CLASS" missed="1" covered="0"/>
                    </sourcefile>
                    <sourcefile name="Calculator.java">
                        <line nr="6" mi="0" ci="2" mb="0" cb="0"/>
                        <line nr="7" mi="0" ci="3" mb="0" cb="0"/>
                        <line nr="8" mi="0" ci="1" mb="0" cb="0"/>
                        <line nr="11" mi="0" ci="6" mb="0" cb="0"/>
                        <line nr="13" mi="0" ci="3" mb="0" cb="0"/>
                        <line nr="17" mi="6" ci="0" mb="0" cb="0"/>
                        <line nr="19" mi="3" ci="0" mb="0" cb="0"/>
                        <counter type="INSTRUCTION" missed="9" covered="15"/>
                        <counter type="LINE" missed="2" covered="5"/>
                        <counter type="COMPLEXITY" missed="1" covered="2"/>
                        <counter type="METHOD" missed="1" covered="2"/>
                        <counter type="CLASS" missed="0" covered="1"/>
                    </sourcefile>
                    <counter type="INSTRUCTION" missed="32" covered="15"/>
                    <counter type="LINE" missed="10" covered="5"/>
                    <counter type="COMPLEXITY" missed="5" covered="2"/>
                    <counter type="METHOD" missed="5" covered="2"/>
                    <counter type="CLASS" missed="1" covered="1"/>
                </package>
                <counter type="INSTRUCTION" missed="32" covered="15"/>
                <counter type="LINE" missed="10" covered="5"/>
                <counter type="COMPLEXITY" missed="5" covered="2"/>
                <counter type="METHOD" missed="5" covered="2"/>
                <counter type="CLASS" missed="1" covered="1"/>
            </report>
        """.trimIndent()

    @Test
    fun `It parses a single Jacoco coverage report with a _report_ root element and a _sessioninfo_ element`() {
        val reports = JacocoXMLParser()(listOf(testReport.byteInputStream()))

        expectThat(reports) hasSize 1
        expectThat(reports[0].testClassName) isEqualTo "com.example.CalculatorTest"
        expectThat(reports[0].testMethodName) isEqualTo "testAddsNumbers"
    }

    @Test
    fun `It does not parse a Jacoco coverage report with multiple _sessioninfo_ elements`() {
        val document = """
            <report name="example">
                <sessioninfo id="com.example.CalculatorTest##testAddsNumbers" start="1605719602562" dump="1605719602565"/>
                <sessioninfo id="com.example.OtherTest##testAddsNumbers" start="1605719602562" dump="1605719602565"/>
            </report>
        """.trimIndent()

        expectCatching { JacocoXMLParser()(listOf(document.byteInputStream())) }
            .isFailure()
            .isA<JacocoXMLParser.JacocoXMLParserException>()
    }

    @Test
    fun `It does not parse a Jacoco coverage report with an invalid _sessioninfo_ id separator`() {
        val document = """
            <report name="example">
                <sessioninfo id="com.example.CalculatorTest[]testAddsNumbers" start="1605719602562" dump="1605719602565"/>
            </report>
        """.trimIndent()

        expectCatching { JacocoXMLParser()(listOf(document.byteInputStream())) }
            .isFailure()
            .isA<JacocoXMLParser.JacocoXMLParserException>()
    }

    @Test
    fun `It does not parse a Jacoco coverage report with a missing _sessioninfo_ id method or class name`() {
        val document = """
            <report name="example">
                <sessioninfo id="com.example.CalculatorTest" start="1605719602562" dump="1605719602565"/>
            </report>
        """.trimIndent()

        expectCatching { JacocoXMLParser()(listOf(document.byteInputStream())) }
            .isFailure()
            .isA<JacocoXMLParser.JacocoXMLParserException>()
    }

    @Test
    fun `It parses a single Jacoco coverage report with a _sourcefile_ elements`() {
        val reports = JacocoXMLParser()(listOf(testReport.byteInputStream()))

        expectThat(reports[0].packages) hasSize 1
        expectThat(reports[0].packages[0].name) isEqualTo "com/example"
        expectThat(reports[0].packages[0].sourceFiles) hasSize 2
        expectThat(reports[0].packages[0].sourceFiles[0].name) isEqualTo "Counter.java"
        expectThat(reports[0].packages[0].sourceFiles[1].name) isEqualTo "Calculator.java"
    }

    @Test
    fun `It parses a single Jacoco coverage report with _line_ elements`() {
        val reports = JacocoXMLParser()(listOf(testReport.byteInputStream()))

        expectThat(reports[0].packages[0].sourceFiles[0].lines) hasSize 8
        expectThat(reports[0].packages[0].sourceFiles[0].lines[0]) isEqualTo LineStatistics(6, 0, 2, 0, 0)
        expectThat(reports[0].packages[0].sourceFiles[0].lines[7]) isEqualTo LineStatistics(20, 0, 1, 0, 0)

        expectThat(reports[0].packages[0].sourceFiles[1].lines) hasSize 7
        expectThat(reports[0].packages[0].sourceFiles[1].lines[0]) isEqualTo LineStatistics(6, 2, 0, 0, 0)
        expectThat(reports[0].packages[0].sourceFiles[1].lines[6]) isEqualTo LineStatistics(19, 0, 3, 0, 0)
    }
}
