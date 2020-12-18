package io.testaxis.backend.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import io.testaxis.backend.Logger
import io.testaxis.backend.config.AppProperties
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import java.security.SignatureException
import java.util.Date

@Service
class TokenProvider(private val appProperties: AppProperties) {

    private val logger by Logger()

    fun createToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserPrincipal
        val expiryDate = Date(Date().time + appProperties.auth.tokenExpirationMsec)

        return Jwts.builder()
            .setSubject(userPrincipal.id.toString())
            .setIssuedAt(Date())
            .setExpiration(expiryDate)
            .signWith(getKey(), SignatureAlgorithm.HS512)
            .compact()
    }

    fun getUserIdFromToken(token: String?) = Jwts.parserBuilder()
        .setSigningKey(getKey())
        .build()
        .parseClaimsJws(token)
        .body.subject.toLong()

    fun validateToken(authToken: String?): Boolean {
        try {
            Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(authToken)
            return true
        } catch (ex: SignatureException) {
            logger.error("Invalid JWT signature")
        } catch (ex: MalformedJwtException) {
            logger.error("Invalid JWT token")
        } catch (ex: ExpiredJwtException) {
            logger.error("Expired JWT token")
        } catch (ex: UnsupportedJwtException) {
            logger.error("Unsupported JWT token")
        } catch (ex: IllegalArgumentException) {
            logger.error("JWT claims string is empty.")
        }
        return false
    }

    private fun getKey() = Keys.hmacShaKeyFor(Decoders.BASE64.decode(appProperties.auth.tokenSecret))
}
