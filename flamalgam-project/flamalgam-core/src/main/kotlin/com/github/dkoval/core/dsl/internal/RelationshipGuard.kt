package com.github.dkoval.core.dsl.internal

import com.github.dkoval.core.event.*
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.ValueState
import org.apache.flink.api.common.state.ValueStateDescriptor
import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.functions.KeySelector
import org.apache.flink.configuration.Configuration
import org.apache.flink.util.Collector
import org.slf4j.Logger
import org.slf4j.LoggerFactory

sealed class RelationshipGuard<K, U>(
        private val parentKeySelector: KeySelector<U, K>,
        name: String) : RichFlatMapFunction<Event<K, U>, RekeyedEvent<K>>() {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val name = "$name-${javaClass.simpleName}"
    private lateinit var relationshipState: ValueState<Pair<Event<K, U>, K>>

    override fun open(parameters: Configuration) {
        relationshipState = runtimeContext.getState(
                ValueStateDescriptor(name, TypeInformation.of(object : TypeHint<Pair<Event<K, U>, K>>() {})))
    }

    override fun flatMap(newEvent: Event<K, U>, out: Collector<RekeyedEvent<K>>) {
        val valueInState = relationshipState.value()

        // ignore stale events
        valueInState?.also { (lastSeenEvent, _) ->
            if (!newEvent.isNewerThan(lastSeenEvent)) {
                logger.debug("Ignoring stale event. New event: {}, last seen event: {}", newEvent, lastSeenEvent)
                return@flatMap
            }
        }

        // update the state of relationship
        val newParentKey = parentKeySelector.getKey(newEvent.value)
        relationshipState.update(newEvent to newParentKey)

        // handle scenario where a child gets attached to the new parent
        valueInState?.also { (_, lastSeenParentKey) ->
            if (newParentKey == null || lastSeenParentKey != newParentKey) {
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

    companion object {
        @JvmStatic
        fun <K, U> forOneToMany(parentKeySelector: KeySelector<U, K>,
                                name: String): RelationshipGuard<K, U> {
            return OneToManyRelationshipGuard(parentKeySelector, name)
        }
    }
}

private class OneToManyRelationshipGuard<K, U>(
        parentKeySelector: KeySelector<U, K>,
        name: String) : RelationshipGuard<K, U>(parentKeySelector, name)