package com.github.dkoval.core.dsl

import com.github.dkoval.core.KeyedEvent
import com.github.dkoval.core.Record
import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.api.java.functions.KeySelector
import org.apache.flink.streaming.api.datastream.DataStream
import java.util.*

class Relationships<K : Comparable<K>, T : Any>(
        private val parentStream: DataStream<Record<K, T>>) {

    private val keyedChildStreams: MutableList<DataStream<KeyedEvent<K>>> = LinkedList()

    fun <U : Any> oneToMany(childStream: DataStream<Record<K, U>>,
                            foreignKeySelector: KeySelector<U, K>,
                            relationship: Relationship.OneToMany<U>): Relationships<K, T> {
        val keyedChildStream = childStream
                .keyBy { it.key }
                .flatMap(RelationshipGuard.oneToMany(foreignKeySelector, relationship.name))
                .name(relationship.name)
                .uid(relationship.name)

        keyedChildStreams.add(keyedChildStream)
        return this
    }

    fun join(): DataStream<KeyedEvent<K>> {
        return parentStream
                .map { KeyedEvent(it.key, it, isParent = true) }
                .union(*keyedChildStreams.toTypedArray())
                .keyBy { it.key }
    }

    fun <R : Any> join(mapper: MapFunction<KeyedEvent<K>, R>): DataStream<R> = TODO()
}