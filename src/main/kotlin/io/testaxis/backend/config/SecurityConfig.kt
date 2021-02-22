package io.testaxis.backend.config

// Based on https://www.callicoder.com/spring-boot-security-oauth2-social-login-part-1

import io.testaxis.backend.security.CustomUserDetailsService
import io.testaxis.backend.security.RestAuthenticationEntryPoint
import io.testaxis.backend.security.TokenAuthenticationFilter
import io.testaxis.backend.security.TokenProvider
import io.testaxis.backend.security.oauth2.CustomOAuth2UserService
import io.testaxis.backend.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository
import io.testaxis.backend.security.oauth2.OAuth2AuthenticationFailureHandler
import io.testaxis.backend.security.oauth2.OAuth2AuthenticationSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.BeanIds
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, jsr250Enabled = true, prePostEnabled = true)
class SecurityConfig(
    val customUserDetailsService: CustomUserDetailsService,
    val customOAuth2UserService: CustomOAuth2UserService,
    val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
    val oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler,
    val tokenProvider: TokenProvider,
) : WebSecurityConfigurerAdapter() {
    @Bean
    fun tokenAuthenticationFilter() = TokenAuthenticationFilter(tokenProvider, customUserDetailsService)

    /*
      By default, Spring OAuth2 uses HttpSessionOAuth2AuthorizationRequestRepository to save
      the authorization request. But, since our service is stateless, we can't save it in
      the session. We'll save the request in a Base64 encoded cookie instead.
    */
    @Bean
    fun cookieAuthorizationRequestRepository() = HttpCookieOAuth2AuthorizationRequestRepository()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean(BeanIds.AUTHENTICATION_MANAGER)
    override fun authenticationManagerBean(): AuthenticationManager = super.authenticationManagerBean()

    override fun configure(authenticationManagerBuilder: AuthenticationManagerBuilder) {
        authenticationManagerBuilder
            .userDetailsService<UserDetailsService?>(customUserDetailsService)
            .passwordEncoder(passwordEncoder())
    }

    override fun configure(http: HttpSecurity?) {
        http {
            cors {}
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            csrf { disable() }
            httpBasic { disable() }
            exceptionHandling {
                authenticationEntryPoint = RestAuthenticationEntryPoint()
            }
            authorizeRequests {
                authorize("/", permitAll)
                authorize("/error", permitAll)
                authorize("/favicon.ico", permitAll)
                authorize("/**/*.png", permitAll)
                authorize("/**/*.gif", permitAll)
                authorize("/**/*.svg", permitAll)
                authorize("/**/*.jpg", permitAll)
                authorize("/**/*.html", permitAll)
                authorize("/**/*.css", permitAll)
                authorize("/**/*.js", permitAll)
                authorize("/**/*.bash", permitAll)

                authorize("/api/v1/auth/**", permitAll)
                authorize("/auth/**", permitAll)
                authorize("/oauth2/**", permitAll)

                authorize("/experiment/**", permitAll)

                authorize(anyRequest, authenticated)
            }
            oauth2Login {
                authorizationEndpoint {
                    baseUri = "/oauth2/authorize"
                    authorizationRequestRepository = cookieAuthorizationRequestRepository()
                }
                redirectionEndpoint {
                    baseUri = "/oauth2/callback/*"
                }
                userInfoEndpoint {
                    userService = customOAuth2UserService
                }
                authenticationSuccessHandler = oAuth2AuthenticationSuccessHandler
                authenticationFailureHandler = oAuth2AuthenticationFailureHandler
            }

            addFilterAt(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)
        }
    }
}
