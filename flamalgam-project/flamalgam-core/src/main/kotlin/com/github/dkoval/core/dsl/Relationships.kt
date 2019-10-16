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

    open fun <CK : Any, CV : Any> and(childStream: DataStream<Event<CK, CV>>): BuildRelationshipStep<PK, PV, CK, CV> {
        return JoinableRelationships(this)
                .and(childStream)
    }

    class BuildRelationshipStep<PK : Any, PV : Any, CK : Any, CV : Any>(
            private val childStream: DataStream<Event<CK, CV>>,
            private val relationships: JoinableRelationships<PK, PV>) {

        fun oneToMany(parentKeySelector: (CV) -> PK?,
                      cardinality: Cardinality.Many<CK, CV>): JoinableRelationships<PK, PV> {

            val rekeyedChildStream = childStream
                    .keyBy({ it.key }, TypeInformation.of(cardinality.keyClass))
                    .flatMap(OneToManyRelationshipGuard(parentKeySelector, cardinality.name))
                    .name(cardinality.name)
                    .uid(cardinality.name)

            relationships.rekeyedChildStreams.add(rekeyedChildStream)
            return relationships
        }
    }

    companion object {
        @JvmStatic
        inline fun <reified PK : Any, PV : Any> between(parentStream: DataStream<Event<PK, PV>>): Relationships<PK, PV> {
            return Relationships(parentStream, PK::class.java)
        }
    }
}

class JoinableRelationships<PK : Any, PV : Any>(
        relationships: Relationships<PK, PV>) : Relationships<PK, PV>(relationships) {

    override fun <CK : Any, CV : Any> and(childStream: DataStream<Event<CK, CV>>): BuildRelationshipStep<PK, PV, CK, CV> {
        return BuildRelationshipStep(childStream, this)
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