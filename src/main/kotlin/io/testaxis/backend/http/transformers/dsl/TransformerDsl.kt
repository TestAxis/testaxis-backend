package io.testaxis.backend.http.transformers.dsl

typealias KeyValueData = Map<String, Any?>
typealias converter<T> = (T) -> KeyValueData

@Suppress("UnnecessaryAbstractClass")
abstract class Transformer {
    private var data = mutableMapOf<String, Any?>()

    protected infix operator fun <T> String.minus(value: T) {
        data[this] = value
    }

    protected fun <T> transform(
        entity: T,
        vararg extendConverters: converter<T>,
        convertData: T.() -> Unit
    ): KeyValueData {
        val extendedConvertersData = extendConverters.fold(mapOf<String, Any?>()) { data, convert ->
            data + convert(entity)
        }

        data = mutableMapOf()
        entity.convertData()

        return extendedConvertersData + data
    }

    fun <T> transform(entities: List<T>, converter: converter<T>) = entities.map(converter)
}
