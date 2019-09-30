package com.github.dkoval.core.dsl

sealed class Relationship<T : Any>(
        open val name: String,
        open val clazz: Class<T>) {

    data class OneToMany<T : Any>(
            override val name: String,
            override val clazz: Class<T>) : Relationship<T>(name, clazz)

    data class ManyToOne<T : Any>(
            override val name: String,
            override val clazz: Class<T>) : Relationship<T>(name, clazz)

    companion object {
        @JvmStatic
        fun <T : Any> oneToMany(name: String, clazz: Class<T>) = OneToMany(name, clazz)

        @JvmStatic
        fun <T : Any> manyToOne(name: String, clazz: Class<T>) = ManyToOne(name, clazz)
    }
}