package com.github.dkoval.core.dsl

import com.github.dkoval.core.Record
import org.apache.flink.streaming.api.datastream.DataStream

open class Relations<K : Comparable<K>, V : Any>(
        internal val stream: DataStream<Record<K, V>>,
        internal val relationship: Relationship.One<V>) {

    constructor(that: Relations<K, V>) : this(that.stream, that.relationship)

    open fun <U : Any> join(stream: DataStream<Record<K, U>>,
                            relationship: Relationship<U>): EstablishedRelations<K, V> {
        return EstablishedRelations(this)
    }

    companion object {
        @JvmStatic
        fun <K : Comparable<K>, V : Any> create(stream: DataStream<Record<K, V>>,
                                                name: String,
                                                clazz: Class<V>): Relations<K, V> {
            return Relations(stream, Relationship.One(name, clazz))
        }
    }
}

fun <K : Comparable<K>, V : Any> DataStream<Record<K, V>>.toRelations(name: String, clazz: Class<V>): Relations<K, V> {
    return Relations.create(this, name, clazz)
}

