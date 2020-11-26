package io.testaxis.backend

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <T> T.ifNull(action: () -> Any): T {
    if (this == null) {
        action()
    }
    return this
}

class Logger<in R : Any> : ReadOnlyProperty<R, Logger> {
    override fun getValue(thisRef: R, property: KProperty<*>): Logger = LoggerFactory.getLogger(thisRef.javaClass)
}
