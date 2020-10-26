package io.testaxis.backend.models

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany

@Entity
data class Project(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
    val slug: String,
    val name: String,
    @JsonIgnore @OneToMany(fetch = FetchType.LAZY) @JoinColumn(name = "project_id")
    val builds: List<Build>? = null,
) {
    companion object {
        fun splitNameFromSlug(slug: String) = slug.split('/')[0]
    }
}
