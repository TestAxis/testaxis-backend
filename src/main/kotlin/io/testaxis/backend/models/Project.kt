package io.testaxis.backend.models

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class Project(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    val name: String
)
