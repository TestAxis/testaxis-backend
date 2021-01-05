package io.testaxis.backend.models

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.validation.constraints.Email

@Entity
@Table(name = "\"user\"", uniqueConstraints = [UniqueConstraint(columnNames = ["email"])])
class User(
    var name: String,
    var email: @Email String,
    var imageUrl: String? = null,
    @JsonIgnore
    var password: String? = null,
    @Enumerated(EnumType.STRING)
    val provider: AuthProvider,
    val providerId: String? = null,
    @JsonIgnore @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL]) @JoinColumn(name = "user_id")
    var projects: MutableList<Project> = mutableListOf(),
) : AbstractJpaPersistable<Long>() {
    companion object {
        // TODO: Remove
        val fake = User(name="haa", email="aa@asf.nl", imageUrl = null, provider = AuthProvider.Local)
    }
}

enum class AuthProvider {
    Local, GitHub
}
