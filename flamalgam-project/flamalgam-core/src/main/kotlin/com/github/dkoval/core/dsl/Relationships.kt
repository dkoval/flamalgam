package com.github.dkoval.core.dsl

import com.github.dkoval.core.dsl.internal.RelationshipGuard
import com.github.dkoval.core.event.Event
import com.github.dkoval.core.event.RekeyedEvent
import com.github.dkoval.core.event.rekey
import org.apache.flink.api.java.functions.KeySelector
import org.apache.flink.streaming.api.datastream.DataStream
import java.util.*

class Relationships<PK, PV>(
        private val parentStream: DataStream<Event<PK, PV>>) {

    private val rekeyedChildStreams: MutableList<DataStream<RekeyedEvent<PK>>> = LinkedList()

    fun <CK, CV> oneToMany(childStream: DataStream<Event<CK, CV>>,
                           parentKeySelector: KeySelector<CV, PK>,
                           relationship: Relationship.OneToMany<CV>): Relationships<PK, PV> {

        val rekeyedChildStream = childStream
                .keyBy { it.key }
                .flatMap(RelationshipGuard.forOneToMany(parentKeySelector, relationship.name))
                .name(relationship.name)
                .uid(relationship.name)

        rekeyedChildStreams.add(rekeyedChildStream)
        return this
    }

    fun join(): DataStream<RekeyedEvent<PK>> {
        val rekeyedParentStream = parentStream
                .map { it.rekey(it.key, true) }

        return rekeyedParentStream
                .union(*rekeyedChildStreams.toTypedArray())
                .keyBy { it.key }
    }
}