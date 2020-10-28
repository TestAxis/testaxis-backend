package io.testaxis.backend.models

import org.springframework.data.util.ProxyUtils
import java.io.Serializable
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MappedSuperclass

/**
 * The base entity for models, providing an identifier and equals/hashcode implementations.
 *
 * Based on: https://kotlinexpertise.com/hibernate-with-kotlin-spring-boot/
 */
@MappedSuperclass
@Suppress("UnnecessaryAbstractClass")
abstract class AbstractJpaPersistable<T : Serializable> {
    companion object {
        private val serialVersionUID = -5554308939380869754L
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    final var id: T? = null
        private set

    override fun equals(other: Any?): Boolean {
        other ?: return false

        if (this === other) return true

        if (javaClass != ProxyUtils.getUserClass(other)) return false

        other as AbstractJpaPersistable<*>

        return if (null == this.id) false else this.id == other.id
    }

    override fun hashCode(): Int {
        return 31
    }

    override fun toString() = "Entity of type ${this.javaClass.name} with id: $id"
}
