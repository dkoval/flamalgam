package com.github.dkoval.core.dsl

import com.github.dkoval.core.*
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

sealed class RelationshipGuard<K : Any, V>(
        private val parentKeySelector: KeySelector<V, K>,
        name: String) : RichFlatMapFunction<Event<*, V>, RekeyedEvent<K>>() {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val name = "$name-${javaClass.simpleName}"
    private lateinit var relationshipState: ValueState<Pair<Event<*, V>, K>>

    override fun open(parameters: Configuration) {
        relationshipState = runtimeContext.getState(
                ValueStateDescriptor(name, TypeInformation.of(object : TypeHint<Pair<Event<*, V>, K>>() {})))
    }

    override fun flatMap(newEvent: Event<*, V>, out: Collector<RekeyedEvent<K>>) {
        // ignore stale events
        val currentValueInState = relationshipState.value()
        currentValueInState?.also {
            val (currentEvent, _) = it
            if (!newEvent.isNewerThan(currentEvent)) {
                logger.debug("Ignoring stale record. New event: {}, current event: {}", newEvent, currentEvent)
                return@flatMap
            }
        }

        // update the state of relationship
        val newParentKey = parentKeySelector.getKey(newEvent.value)
        relationshipState.update(newEvent to newParentKey)

        // handle relationship change
        currentValueInState?.also {
            val (_, currentParentKey) = it
            if (newParentKey == null || currentParentKey != newParentKey) {
                // emit `relationship discarded` event
                val event = RelationshipDiscardedEvent(newEvent.key, newEvent.version, currentParentKey)
                        .rekey(currentParentKey)
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
        fun <K : Any, V> oneToMany(parentKeySelector: KeySelector<V, K>,
                                   name: String): RelationshipGuard<K, V> {
            return OneToManyRelationshipGuard(parentKeySelector, name)
        }
    }
}

private class OneToManyRelationshipGuard<K : Any, V>(
        parentKeySelector: KeySelector<V, K>,
        name: String) : RelationshipGuard<K, V>(parentKeySelector, name)