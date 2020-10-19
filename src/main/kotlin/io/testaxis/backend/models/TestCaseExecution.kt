package io.testaxis.backend.models

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.util.Date
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.Lob
import javax.persistence.ManyToOne

@Entity
data class TestCaseExecution(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    @ManyToOne @JoinColumn(name = "build_id") val build: Build,
    val testSuiteName: String,
    val name: String,
    val className: String,
    val time: Double,
    val passed: Boolean,
    @Lob val failureMessage: String?,
    val failureType: String?,
    @Lob val failureContent: String?,
    @CreatedDate val createdAt: Date = Date(),
    @Suppress("ForbiddenComment") // TODO: Fix @CreatedDate and @LastModifiedDate annotations
    @LastModifiedDate val updatedAt: Date = Date(),
)
