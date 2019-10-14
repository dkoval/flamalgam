package com.github.dkoval.core.dsl.internal

import com.github.dkoval.core.event.*
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.ValueState
import org.apache.flink.api.common.state.ValueStateDescriptor
import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.util.Collector
import org.slf4j.Logger
import org.slf4j.LoggerFactory

sealed class RelationshipGuard<CK, CV, PK>(
        private val parentKeySelector: (CV) -> PK,
        name: String) : RichFlatMapFunction<Event<CK, CV>, RekeyedEvent<PK>>() {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val name = "$name-${javaClass.simpleName}"
    private lateinit var relationshipState: ValueState<Pair<Event<CK, CV>, PK?>>

    override fun open(parameters: Configuration) {
        relationshipState = runtimeContext.getState(
                ValueStateDescriptor(name, TypeInformation.of(object : TypeHint<Pair<Event<CK, CV>, PK?>>() {})))
    }

    override fun flatMap(newEvent: Event<CK, CV>, out: Collector<RekeyedEvent<PK>>) {
        val valueInState = relationshipState.value()

        // ignore stale events
        valueInState?.also { (lastSeenEvent, _) ->
            if (!newEvent.isNewerThan(lastSeenEvent)) {
                logger.debug("Ignoring stale event. New event: {}, last seen event: {}", newEvent, lastSeenEvent)
                return@flatMap
            }
        }

        // update the state of relationship
        val newParentKey = newEvent.value?.let(parentKeySelector)
        relationshipState.update(newEvent to newParentKey)

        // handle scenario where a child gets attached to the new parent
        valueInState?.also { (_, lastSeenParentKey) ->
            if ((lastSeenParentKey != null) && (newParentKey == null || lastSeenParentKey != newParentKey)) {
                // emit `relationship discarded` event
                val event = RelationshipDiscardedEvent(
                        newEvent.key, newEvent.version, lastSeenParentKey).rekey(lastSeenParentKey)
                out.collect(event)
            }
        }

        // emit regular event
        newParentKey?.also {
            val event = newEvent.rekey(it)
            out.collect(event)
        }
    }
}

class OneToManyRelationshipGuard<CK, CV, PK>(
        parentKeySelector: (CV) -> PK,
        name: String) : RelationshipGuard<CK, CV, PK>(parentKeySelector, name)