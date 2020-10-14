package io.testaxis.backend.models

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
