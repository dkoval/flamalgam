package com.github.dkoval.core.dsl

import com.github.dkoval.core.Record
import org.apache.flink.streaming.api.datastream.DataStream

class Relationships<K : Comparable<K>, V : Any>(
        private val parentStream: DataStream<Record<K, V>>,
        private val parentCardinality: Cardinality.One<V>) {

    fun <U : Any> oneToMany(stream: DataStream<Record<K, U>>,
                            cardinality: Cardinality.Many<U>): Relationships<K, V> {
        TODO()
    }

    fun <U : Any> manyToOne(stream: DataStream<Record<K, U>>,
                            cardinality: Cardinality.One<U>): Relationships<K, V> {
        TODO()
    }

    fun join(): JoinResult<V> {
        TODO()
    }

    companion object {
        @JvmStatic
        fun <K : Comparable<K>, V : Any> create(block: () -> Relationships<K, V>) = block()

        @JvmStatic
        fun <K : Comparable<K>, V : Any> between(stream: DataStream<Record<K, V>>,
                                                 name: String,
                                                 clazz: Class<V>): Relationships<K, V> {
            return between(stream, Cardinality.One(name, clazz))
        }

        @JvmStatic
        fun <K : Comparable<K>, V : Any> between(stream: DataStream<Record<K, V>>,
                                                 cardinality: Cardinality.One<V>): Relationships<K, V> {
            return Relationships(stream, cardinality)
        }
    }
}

fun <K : Comparable<K>, V : Any> relationships(block: () -> Relationships<K, V>) = Relationships.create(block)