package io.testaxis.backend.security

import io.testaxis.backend.exceptions.ResourceNotFoundException
import io.testaxis.backend.repositories.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(val userRepository: UserRepository) : UserDetailsService {
    @Transactional
    override fun loadUserByUsername(email: String) = UserPrincipal.create(
        userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("User not found with email : $email")
    )

    @Transactional
    fun loadUserById(id: Long) =
        UserPrincipal.create(userRepository.findByIdOrNull(id) ?: throw ResourceNotFoundException())
}
