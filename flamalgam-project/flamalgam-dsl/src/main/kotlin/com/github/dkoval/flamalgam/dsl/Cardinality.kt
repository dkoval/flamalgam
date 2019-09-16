package com.github.dkoval.flamalgam.dsl

sealed class Cardinality<V : Any>(
        open val name: String,
        open val clazz: Class<V>) {

    data class One<V : Any>(
            override val name: String,
            override val clazz: Class<V>) : Cardinality<V>(name, clazz)

    data class Many<V : Any>(
            override val name: String,
            override val clazz: Class<V>) : Cardinality<V>(name, clazz)

    companion object {
        fun <V : Any> one(name: String, clazz: Class<V>) = One(name, clazz)
        fun <V : Any> many(name: String, clazz: Class<V>) = Many(name, clazz)
    }
}