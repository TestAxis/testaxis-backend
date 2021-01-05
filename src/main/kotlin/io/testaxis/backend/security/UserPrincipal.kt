package io.testaxis.backend.security

import io.testaxis.backend.models.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User

class UserPrincipal(
    val id: Long,
    val email: String,
    private val password: String?,
    private val authorities: Collection<GrantedAuthority>
) : OAuth2User, UserDetails {

    private var attributes: Map<String, Any> = emptyMap()

    override fun getPassword() = password

    override fun getUsername() = email

    override fun isAccountNonExpired() = true

    override fun isAccountNonLocked() = true

    override fun isCredentialsNonExpired() = true

    override fun isEnabled() = true

    override fun getAuthorities() = authorities

    override fun getAttributes() = attributes

    override fun getName() = id.toString()

    companion object {
        fun create(user: User) = UserPrincipal(
            user.id ?: error("User id is not present."),
            user.email,
            user.password,
            authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        fun create(user: User, attributes: Map<String, Any>) = create(user).apply { this.attributes = attributes }
    }
}
