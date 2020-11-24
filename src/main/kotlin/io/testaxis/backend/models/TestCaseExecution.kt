package io.testaxis.backend.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.util.Date
import javax.persistence.AttributeConverter
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.Lob
import javax.persistence.ManyToOne

@Entity
class TestCaseExecution(
    @ManyToOne @JoinColumn(name = "build_id") var build: Build,
    var testSuiteName: String,
    var name: String,
    var className: String,
    var time: Double,
    var passed: Boolean,
    @Lob var failureMessage: String?,
    var failureType: String?,
    @Lob var failureContent: String?,
    @Lob @Transient @Convert(converter = HashMapConverter::class)
    var coveredLines: MutableMap<String, List<Int>> = mutableMapOf(),
    @CreatedDate var createdAt: Date = Date(),
    @Suppress("ForbiddenComment") // TODO: Fix @CreatedDate and @LastModifiedDate annotations
    @LastModifiedDate var updatedAt: Date = Date(),
) : AbstractJpaPersistable<Long>()

private class HashMapConverter<T1, T2> : AttributeConverter<MutableMap<T1, T2>, String?> {
    override fun convertToDatabaseColumn(attribute: MutableMap<T1, T2>): String? =
        jacksonObjectMapper().writeValueAsString(attribute).also { println("aa $attribute - $it") }

    override fun convertToEntityAttribute(dbData: String?): MutableMap<T1, T2> =
        if (dbData == null) mutableMapOf() else jacksonObjectMapper().readValue(dbData)
}
