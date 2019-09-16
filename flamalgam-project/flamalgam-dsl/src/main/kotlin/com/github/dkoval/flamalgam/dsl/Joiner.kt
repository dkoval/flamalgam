package com.github.dkoval.flamalgam.dsl

import com.github.dkoval.core.Record
import org.apache.flink.streaming.api.datastream.DataStream

class Joiner<K : Comparable<K>, V : Any>(
        private val parentStream: DataStream<Record<K, V>>,
        private val parentCardinality: Cardinality.One<V>) {

    fun <U : Any> oneToMany(childStream: DataStream<Record<K, U>>,
                            cardinality: Cardinality.Many<U>): Joiner<K, V> {
        TODO()
    }

    fun <U : Any> manyToOne(childStream: DataStream<Record<K, U>>,
                            cardinality: Cardinality.One<U>): Joiner<K, V> {
        TODO()
    }
}

fun <K : Comparable<K>, V : Any> DataStream<Record<K, V>>.relationships(
        descriptor: Cardinality.One<V>,
        block: Joiner<K, V>.() -> Unit): Joiner<K, V> = Joiner(this, descriptor).apply(block)