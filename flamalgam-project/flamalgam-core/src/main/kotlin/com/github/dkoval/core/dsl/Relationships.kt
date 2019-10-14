package com.github.dkoval.core.dsl

import com.github.dkoval.core.dsl.internal.OneToManyRelationshipGuard
import com.github.dkoval.core.event.Event
import com.github.dkoval.core.event.RekeyedEvent
import com.github.dkoval.core.event.rekey
import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.datastream.DataStream
import java.util.*

class Relationships<PK, PV>(
        private val parentStream: DataStream<Event<PK, PV>>,
        private val parentKeyClass: Class<PK>) {

    private val rekeyedChildStreams: MutableList<DataStream<RekeyedEvent<PK>>> = LinkedList()

    fun <CK, CV> oneToMany(childStream: DataStream<Event<CK, CV>>,
                           parentKeySelector: (CV) -> PK,
                           relationship: Relationship.OneToMany<CK, CV>): Relationships<PK, PV> {
        val rekeyedChildStream = childStream
                .keyBy({ it.key }, TypeInformation.of(relationship.keyClass))
                .flatMap(OneToManyRelationshipGuard(parentKeySelector, relationship.name))
                .name(relationship.name)
                .uid(relationship.name)

        rekeyedChildStreams.add(rekeyedChildStream)
        return this
    }

    fun join(): DataStream<RekeyedEvent<PK>> {
        val rekeyedParentStream = parentStream
                .map { it.rekey(it.key, true) }
                .returns(TypeInformation.of(object : TypeHint<RekeyedEvent<PK>>() {}))

        return rekeyedParentStream
                .union(*this.rekeyedChildStreams.toTypedArray())
                .keyBy({ it.key }, TypeInformation.of(parentKeyClass))
    }

    companion object {
        @JvmStatic
        inline fun <reified PK, PV> withParent(parentStream: DataStream<Event<PK, PV>>): Relationships<PK, PV> {
            return Relationships(parentStream, PK::class.java)
        }
    }
}