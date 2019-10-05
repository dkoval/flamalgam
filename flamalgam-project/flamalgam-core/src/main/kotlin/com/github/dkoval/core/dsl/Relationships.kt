package com.github.dkoval.core.dsl

import com.github.dkoval.core.dsl.internal.RelationshipGuard
import com.github.dkoval.core.dsl.internal.StatefulResultMapper
import com.github.dkoval.core.event.Event
import com.github.dkoval.core.event.RekeyedEvent
import com.github.dkoval.core.event.rekey
import org.apache.flink.api.java.functions.KeySelector
import org.apache.flink.streaming.api.datastream.DataStream
import java.util.*

class Relationships<K : Any, V : Any>(
        private val parentStream: DataStream<Event<K, V>>) {

    private val rekeyedChildStreams: MutableList<DataStream<RekeyedEvent<K>>> = LinkedList()

    fun <U> oneToMany(childStream: DataStream<Event<*, U>>,
                      parentKeySelector: KeySelector<U, K>,
                      relationship: Relationship.OneToMany<U>): Relationships<K, V> {

        val rekeyedChildStream = childStream
                .keyBy { it.key }
                .flatMap(RelationshipGuard.oneToMany(parentKeySelector, relationship.name))
                .name(relationship.name)
                .uid(relationship.name)

        rekeyedChildStreams.add(rekeyedChildStream)
        return this
    }

    fun join(): DataStream<RekeyedEvent<K>> {
        val rekeyedParentStream = parentStream
                .map { it.rekey(it.key, true) }

        return rekeyedParentStream
                .union(*rekeyedChildStreams.toTypedArray())
                .keyBy { it.key }
    }

    fun <R> join(mapper: (Result<V>) -> Event<K, R>,
                 name: String): DataStream<Event<K, R>> {

        return join()
                .flatMap(StatefulResultMapper(mapper))
                .name(name)
                .uid(name)
    }
}