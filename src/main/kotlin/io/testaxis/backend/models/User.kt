package io.testaxis.backend.models

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
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
    val providerId: String? = null
) : AbstractJpaPersistable<Long>()

enum class AuthProvider {
    Local, GitHub
}
