package io.testaxis.backend

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.json.JsonParserFactory
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.result.ContentResultMatchers
import strikt.api.expectThat
import strikt.assertions.containsKey
import java.nio.charset.StandardCharsets

fun ContentResultMatchers.jsonContent(value: Any) = json(ObjectMapper().writeValueAsString(value))

fun ContentResultMatchers.jsonContent(vararg values: Any) = jsonContent(values)

fun ContentResultMatchers.hasValidationError(key: String) = ResultMatcher { result: MvcResult ->
    val content = result.response.getContentAsString(StandardCharsets.UTF_8)

    val parsedJson = JsonParserFactory.getJsonParser().parseMap(content)

    expectThat(parsedJson).containsKey(key)
}
