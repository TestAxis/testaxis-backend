package io.testaxis.backend.models

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.util.Date
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
    @CreatedDate var createdAt: Date = Date(),
    @Suppress("ForbiddenComment") // TODO: Fix @CreatedDate and @LastModifiedDate annotations
    @LastModifiedDate var updatedAt: Date = Date(),
) : AbstractJpaPersistable<Long>()
