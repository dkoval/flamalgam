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
        fun <T : Any> oneToMany(name: String, clazz: Class<T>) = OneToMany(name, clazz)

        @JvmStatic
        fun <T : Any> manyToOne(name: String, clazz: Class<T>) = ManyToOne(name, clazz)
    }
}