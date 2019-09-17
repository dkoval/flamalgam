package com.github.dkoval.core.dsl

sealed class Cardinality<T : Any>(
        open val name: String,
        open val clazz: Class<T>) {

    data class One<T : Any>(
            override val name: String,
            override val clazz: Class<T>) : Cardinality<T>(name, clazz)

    data class Many<T : Any>(
            override val name: String,
            override val clazz: Class<T>) : Cardinality<T>(name, clazz)

    companion object {
        @JvmStatic
        fun <T : Any> one(name: String, clazz: Class<T>) = One(name, clazz)

        @JvmStatic
        fun <T : Any> many(name: String, clazz: Class<T>) = Many(name, clazz)
    }
}