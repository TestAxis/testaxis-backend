package io.testaxis.backend.exceptions

import org.springframework.http.HttpStatus
import org.springframework.validation.BindException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import javax.validation.ConstraintViolationException

@ControllerAdvice
@RestControllerAdvice
object ExceptionHandlers {
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidExceptions(exception: MethodArgumentNotValidException) =
        parseFieldErrors(exception.bindingResult.allErrors)

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationExceptions(exception: ConstraintViolationException) =
        exception.constraintViolations.map {
            it.propertyPath.toString() to it.message
        }.toMap()

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(BindException::class)
    fun handleBindExceptions(bindingResult: BindingResult) = parseFieldErrors(bindingResult.allErrors)

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(CustomValidationException::class)
    fun handleCustomValidationExceptions(exception: CustomValidationException) = parseFieldErrors(exception.errors)

    private fun parseFieldErrors(errors: List<ObjectError>) = errors.filterIsInstance<FieldError>().map {
        it.field to it.defaultMessage
    }.toMap()
}
