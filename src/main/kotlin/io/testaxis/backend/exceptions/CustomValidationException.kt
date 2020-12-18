package io.testaxis.backend.exceptions

import org.springframework.http.HttpStatus
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
class CustomValidationException(key: String, message: String) :
    RuntimeException("Validation error: Field [$key] $message") {

    val errors = listOf(FieldError("", key, message))
}
