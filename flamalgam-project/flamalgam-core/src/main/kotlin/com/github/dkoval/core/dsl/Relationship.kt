package com.github.dkoval.core.dsl

sealed class Relationship<K, V> {
    abstract val name: String
    abstract val keyClass: Class<K>
    abstract val valueClass: Class<V>

    data class OneToMany<K, V>(
            override val name: String,
            override val keyClass: Class<K>,
            override val valueClass: Class<V>) : Relationship<K, V>()

    data class ManyToOne<K, V>(
            override val name: String,
            override val keyClass: Class<K>,
            override val valueClass: Class<V>) : Relationship<K, V>()

    companion object {
        @JvmStatic
        inline fun <reified K, reified V> oneToMany(name: String) = OneToMany(name, K::class.java, V::class.java)

        @JvmStatic
        inline fun <reified K, reified T> manyToOne(name: String) = ManyToOne(name, K::class.java, T::class.java)
    }
}