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

sealed class RelationshipGuard<CK, CV, PK>(
        private val parentKeySelector: KeySelector<CV, PK>,
        name: String) : RichFlatMapFunction<Event<CK, CV>, RekeyedEvent<PK>>() {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val name = "$name-${javaClass.simpleName}"
    private lateinit var relationshipState: ValueState<Pair<Event<CK, CV>, PK>>

    override fun open(parameters: Configuration) {
        relationshipState = runtimeContext.getState(
                ValueStateDescriptor(name, TypeInformation.of(object : TypeHint<Pair<Event<CK, CV>, PK>>() {})))
    }

    override fun flatMap(newEvent: Event<CK, CV>, out: Collector<RekeyedEvent<PK>>) {
        val valueInState = relationshipState.value()

        // ignore stale events
        valueInState?.also {
            val (oldEvent, _) = it
            if (!newEvent.isNewerThan(oldEvent)) {
                logger.debug("Ignoring stale record. New event: {}, current event: {}", newEvent, oldEvent)
                return@flatMap
            }
        }

        // update the state of relationship
        val newParentKey = parentKeySelector.getKey(newEvent.value)
        relationshipState.update(newEvent to newParentKey)

        // handle relationship change
        valueInState?.also {
            val (_, oldParentKey) = it
            if (newParentKey == null || oldParentKey != newParentKey) {
                // emit `relationship discarded` event
                val event = RelationshipDiscardedEvent(newEvent.key, newEvent.version, oldParentKey)
                        .rekey(oldParentKey)
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
        fun <CK, CV, PK> forOneToMany(parentKeySelector: KeySelector<CV, PK>,
                                      name: String): RelationshipGuard<CK, CV, PK> {
            return OneToManyRelationshipGuard(parentKeySelector, name)
        }
    }
}

private class OneToManyRelationshipGuard<CK, PV, PK>(
        parentKeySelector: KeySelector<PV, PK>,
        name: String) : RelationshipGuard<CK, PV, PK>(parentKeySelector, name)