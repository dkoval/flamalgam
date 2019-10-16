package com.github.dkoval.core.dsl

/**
 * Cardinality refers to the maximum number of times an instance of one [Event][com.github.dkoval.core.event.Event]
 * can relate to instances of another event.
 */
sealed class Cardinality<K, V> {
    abstract val name: String
    abstract val keyClass: Class<K>
    abstract val valueClass: Class<V>

    data class Many<K, V>(
            override val name: String,
            override val keyClass: Class<K>,
            override val valueClass: Class<V>) : Cardinality<K, V>()

    data class One<K, V>(
            override val name: String,
            override val keyClass: Class<K>,
            override val valueClass: Class<V>) : Cardinality<K, V>()

    companion object {
        @JvmStatic
        inline fun <reified K, reified V> many(name: String) = Many(name, K::class.java, V::class.java)

        @JvmStatic
        inline fun <reified K, reified T> one(name: String) = One(name, K::class.java, T::class.java)
    }
}