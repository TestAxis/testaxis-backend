package io.testaxis.backend.models

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.OneToMany

@Entity
class Project(
    var slug: String,
    var name: String,
    @JsonIgnore @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL]) @JoinColumn(name = "project_id")
    var builds: MutableList<Build> = mutableListOf(),
) : AbstractJpaPersistable<Long>() {
    companion object {
        fun splitNameFromSlug(slug: String) = slug.split('/')[0]
    }
}
