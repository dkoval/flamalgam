package com.github.dkoval.core.dsl

sealed class Relationship<T> {
    abstract val name: String
    abstract val clazz: Class<T>

    data class OneToMany<T>(
            override val name: String,
            override val clazz: Class<T>) : Relationship<T>()

    data class ManyToOne<T>(
            override val name: String,
            override val clazz: Class<T>) : Relationship<T>()

    companion object {
        @JvmStatic
        inline fun <reified T> oneToMany(name: String) = OneToMany(name, T::class.java)

        @JvmStatic
        inline fun <reified T> manyToOne(name: String) = ManyToOne(name, T::class.java)
    }
}