package com.github.dkoval.core.dsl

import com.github.dkoval.core.dsl.internal.OneToManyRelationshipGuard
import com.github.dkoval.core.event.Event
import com.github.dkoval.core.event.RekeyedEvent
import com.github.dkoval.core.event.rekey
import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.datastream.DataStream
import java.util.*

open class Relationships<PK : Any, PV : Any> protected constructor(
        protected val parentStream: DataStream<Event<PK, PV>>,
        protected val parentKeyClass: Class<PK>) {

    protected constructor(other: Relationships<PK, PV>) : this(other.parentStream, other.parentKeyClass)

    protected val rekeyedChildStreams: MutableList<DataStream<RekeyedEvent<PK>>> = LinkedList()

    open fun <CK : Any, CV : Any> oneToMany(childStream: DataStream<Event<CK, CV>>,
                                            parentKeySelector: (CV) -> PK?,
                                            cardinality: Cardinality.Many<CK, CV>): JoinableRelationships<PK, PV> {
        return JoinableRelationships(this)
                .oneToMany(childStream, parentKeySelector, cardinality)
    }

    companion object {
        @JvmStatic
        inline fun <reified PK : Any, PV : Any> withParent(parentStream: DataStream<Event<PK, PV>>): Relationships<PK, PV> {
            return Relationships(parentStream, PK::class.java)
        }
    }
}

class JoinableRelationships<PK : Any, PV : Any>(
        relationships: Relationships<PK, PV>
) : Relationships<PK, PV>(relationships) {

    override fun <CK : Any, CV : Any> oneToMany(childStream: DataStream<Event<CK, CV>>,
                                                parentKeySelector: (CV) -> PK?,
                                                cardinality: Cardinality.Many<CK, CV>): JoinableRelationships<PK, PV> {
        val rekeyedChildStream = childStream
                .keyBy({ it.key }, TypeInformation.of(cardinality.keyClass))
                .flatMap(OneToManyRelationshipGuard(parentKeySelector, cardinality.name))
                .name(cardinality.name)
                .uid(cardinality.name)

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
}