package io.testaxis.backend.http

import org.springframework.web.multipart.MultipartFile
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.Payload
import kotlin.properties.Delegates
import kotlin.reflect.KClass

/**
 * Validation annotation to validate that files are of the correct (given) type.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
@Constraint(validatedBy = [FilesHaveTypeValidator::class])
annotation class FilesHaveType(
    val types: Array<String>,
    val message: String = "One of the given files not of the correct type.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Validator implementation for [FilesHaveType].
 */
class FilesHaveTypeValidator : ConstraintValidator<FilesHaveType, Array<MultipartFile>> {
    private lateinit var types: List<String>

    override fun initialize(annotation: FilesHaveType) {
        types = annotation.types.asList()
    }

    override fun isValid(files: Array<MultipartFile>, context: ConstraintValidatorContext) =
        files.all { types.contains(it.contentType) }
}

/**
 * Validation annotation to validate that files do not exceed the given limit (in bytes).
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
@Constraint(validatedBy = [FilesHaveMaxSizeValidator::class])
annotation class FilesHaveMaxSize(
    val size: Long,
    val message: String = "One of the given files is too large.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Validator implementation for [FilesHaveMaxSize].
 */
class FilesHaveMaxSizeValidator : ConstraintValidator<FilesHaveMaxSize, Array<MultipartFile>> {
    private var size by Delegates.notNull<Long>()

    override fun initialize(annotation: FilesHaveMaxSize) {
        size = annotation.size
    }

    override fun isValid(files: Array<MultipartFile>, context: ConstraintValidatorContext) =
        files.all { it.size < size }
}
