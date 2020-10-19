package io.testaxis.backend.models

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.util.Date
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany

@Entity
data class Build(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    val branch: String = "",
    val commit: String = "",
    val slug: String = "",
    val tag: String? = null,
    val pr: String? = null,
    val service: String? = null,
    val serviceBuild: String? = null,
    val serviceBuildUrl: String? = null,
    val serviceJob: String? = null,
    @OneToMany(fetch = FetchType.EAGER) @JoinColumn(name = "build_id")
    val testCaseExecutions: List<TestCaseExecution> = emptyList(),
    @CreatedDate val createdAt: Date = Date(),
    @Suppress("ForbiddenComment") // TODO: Fix @CreatedDate and @LastModifiedDate annotations
    @LastModifiedDate val updatedAt: Date = Date(),
)
