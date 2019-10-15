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

sealed class RelationshipGuard<CK : Any, CV : Any, PK : Any>(
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
        logger.debug("New event received: {}", newEvent)
        val valueInState = relationshipState.value()

        logger.debug("Value in state: {}", valueInState)
        val oldEvent = valueInState?.first

        // ignore stale events
        if (oldEvent != null && !newEvent.isNewerThan(oldEvent)) {
            logger.debug("Ignoring stale event. New event: {}, last seen event: {}", newEvent, oldEvent)
            return
        }

        // update the state of relationship
        val oldParentKey = valueInState?.second
        val newParentKey = if (newEvent is DeleteEvent<CK, CV>) oldParentKey else newEvent.value?.let(parentKeySelector)
        relationshipState.update(newEvent to newParentKey)

        // handle scenario where a child gets attached to the new parent
        oldParentKey?.also {
            if (newParentKey == null || it != newParentKey) {
                // emit `relationship discarded` event
                val event = RelationshipDiscardedEvent(
                        newEvent.key, newEvent.version, newEvent.valueClass, it).rekey(it)
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

class OneToManyRelationshipGuard<CK : Any, CV : Any, PK : Any>(
        parentKeySelector: (CV) -> PK,
        name: String) : RelationshipGuard<CK, CV, PK>(parentKeySelector, name)