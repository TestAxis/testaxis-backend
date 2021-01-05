package io.testaxis.backend.security.oauth2

import org.springframework.util.SerializationUtils
import java.util.Base64
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

fun HttpServletRequest.getCookie(name: String) = this.cookies?.find { it.name == name }

fun HttpServletResponse.addCookie(name: String, value: String, maxAge: Int) {
    addCookie(
        Cookie(name, value).apply {
            path = "/"
            isHttpOnly = true
            this.maxAge = maxAge
        }
    )
}

fun HttpServletResponse.addSerializedCookie(name: String, valueObject: Any?, maxAge: Int) {
    val value = Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(valueObject))

    addCookie(name, value, maxAge)
}

fun HttpServletResponse.deleteCookie(cookie: Cookie?) {
    addCookie(
        (cookie ?: return).apply {
            value = ""
            path = "/"
            maxAge = 0
        }
    )
}

inline fun <reified T> Cookie.deserialize(): T = T::class.java.cast(
    SerializationUtils.deserialize(Base64.getUrlDecoder().decode(value))
)
