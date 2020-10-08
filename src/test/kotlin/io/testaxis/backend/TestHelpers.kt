package io.testaxis.backend

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.web.servlet.result.ContentResultMatchers

fun ContentResultMatchers.jsonContent(value: Any) = json(ObjectMapper().writeValueAsString(value))

fun ContentResultMatchers.jsonContent(vararg values: Any) = jsonContent(values)
