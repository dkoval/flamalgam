package com.github.dkoval.core.dsl

import com.github.dkoval.core.dsl.internal.RelationshipGuard
import com.github.dkoval.core.event.Event
import com.github.dkoval.core.event.RekeyedEvent
import com.github.dkoval.core.event.rekey
import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.functions.KeySelector
import org.apache.flink.streaming.api.datastream.DataStream
import java.util.*

class Relationships<K, V>(
        private val parentStream: DataStream<Event<K, V>>,
        private val keyClass: Class<K>) {

    private val rekeyedChildStreams: MutableList<DataStream<RekeyedEvent<K>>> = LinkedList()

    fun <U> oneToMany(childStream: DataStream<Event<K, U>>,
                      parentKeySelector: KeySelector<U, K>,
                      relationship: Relationship.OneToMany<U>): Relationships<K, V> {

        val rekeyedChildStream = childStream
                .keyBy({ it.key }, TypeInformation.of(keyClass))
                .flatMap(RelationshipGuard.forOneToMany(parentKeySelector, relationship.name))
                .name(relationship.name)
                .uid(relationship.name)

        rekeyedChildStreams.add(rekeyedChildStream)
        return this
    }

    fun join(): DataStream<RekeyedEvent<K>> {
        val rekeyedParentStream = parentStream
                .map { it.rekey(it.key, true) }
                .returns(TypeInformation.of(object : TypeHint<RekeyedEvent<K>>() {}))

        return rekeyedParentStream
                .union(*this.rekeyedChildStreams.toTypedArray())
                .keyBy({ it.key }, TypeInformation.of(keyClass))
    }

    companion object {
        @JvmStatic
        inline fun <reified K, V> parent(parentStream: DataStream<Event<K, V>>): Relationships<K, V> =
                Relationships(parentStream, K::class.java)
    }
}