package com.github.dkoval.core.dsl

import com.github.dkoval.core.Record
import org.apache.flink.streaming.api.datastream.DataStream

class Relationships<K : Comparable<K>, V : Any>(
        private val parentStream: DataStream<Record<K, V>>,
        private val parentCardinality: Cardinality.One<V>) {

    fun <U : Any> oneToMany(childStream: DataStream<Record<K, U>>,
                            cardinality: Cardinality.Many<U>): Relationships<K, V> {
        TODO()
    }

    fun <U : Any> manyToOne(childStream: DataStream<Record<K, U>>,
                            cardinality: Cardinality.One<U>): Relationships<K, V> {
        TODO()
    }

    fun join(): JoinResult<V> {
        TODO()
    }
}

fun <K : Comparable<K>, V : Any> DataStream<Record<K, V>>.relationships(
        cardinality: Cardinality.One<V>,
        block: Relationships<K, V>.() -> Unit): JoinResult<V> = Relationships(this, cardinality).apply(block).join()