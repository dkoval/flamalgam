package com.github.dkoval.core.dsl

import com.github.dkoval.core.Record
import org.apache.flink.streaming.api.datastream.DataStream

class Relations<K : Comparable<K>, V : Any>(
        private val parentStream: DataStream<Record<K, V>>,
        private val parentCardinality: Cardinality.One<V>) {

    fun <U : Any> oneToMany(stream: DataStream<Record<K, U>>,
                            cardinality: Cardinality.Many<U>): Relations<K, V> {
        TODO()
    }

    fun <U : Any> manyToOne(stream: DataStream<Record<K, U>>,
                            cardinality: Cardinality.One<U>): Relations<K, V> {
        TODO()
    }

    fun join(): JoinResult<V> {
        TODO()
    }

    companion object {
        @JvmStatic
        fun <K : Comparable<K>, V : Any> create(block: () -> Relations<K, V>) = block()

        @JvmStatic
        fun <K : Comparable<K>, V : Any> parent(stream: DataStream<Record<K, V>>,
                                                name: String,
                                                clazz: Class<V>): Relations<K, V> {
            return parent(stream, Cardinality.One(name, clazz))
        }

        @JvmStatic
        fun <K : Comparable<K>, V : Any> parent(stream: DataStream<Record<K, V>>,
                                                cardinality: Cardinality.One<V>): Relations<K, V> {
            return Relations(stream, cardinality)
        }
    }
}

fun <K : Comparable<K>, V : Any> relations(block: () -> Relations<K, V>) = Relations.create(block)