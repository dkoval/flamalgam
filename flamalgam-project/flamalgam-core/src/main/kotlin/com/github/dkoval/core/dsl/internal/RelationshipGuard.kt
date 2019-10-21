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

sealed class RelationshipGuard<K : Any, V : Any, FK : Any, R : InternalEvent<*, *>>(
        private val foreignKeySelector: (V) -> FK?,
        name: String) : RichFlatMapFunction<LifecycleEvent<K, V>, R>() {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val name = "$name-${javaClass.simpleName}"
    private lateinit var relationshipState: ValueState<Pair<NoValueEvent<K, V>, FK?>>

    override fun open(parameters: Configuration) {
        relationshipState = runtimeContext.getState(
                ValueStateDescriptor(name, TypeInformation.of(object : TypeHint<Pair<NoValueEvent<K, V>, FK?>>() {})))
    }

    override fun flatMap(newEvent: LifecycleEvent<K, V>, collector: Collector<R>) {
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
        val oldForeignKey = valueInState?.second
        val newForeignKey = if (newEvent is DeleteEvent<K, V>) oldForeignKey else newEvent.value?.let(foreignKeySelector)
        relationshipState.update(newEvent.withoutValue() to newForeignKey)

        // handle scenario where a child gets attached to the new parent
        oldForeignKey?.also {
            if (newForeignKey == null || it != newForeignKey) {
                // emit `relationship discarded` event
                val out = mapRelationship(newEvent.discardRelationship(), it)
                collector.collect(out)
            }
        }

        newForeignKey?.also {
            // emit regular event
            val out = mapRelationship(newEvent, it)
            collector.collect(out)
        }
    }

    protected abstract fun mapRelationship(event: Event<K, V>, foreignKey: FK): R
}

class OneToManyRelationshipGuard<K : Any, V : Any, FK : Any>(
        foreignKeySelector: (V) -> FK?,
        name: String) : RelationshipGuard<K, V, FK, RekeyedEvent<FK>>(foreignKeySelector, name) {

    override fun mapRelationship(event: Event<K, V>, foreignKey: FK): RekeyedEvent<FK> {
        return event.rekey(foreignKey)
    }
}

class ManyToOneRelationshipGuard<K : Any, V : Any, FK : Any>(
        foreignKeySelector: (V) -> FK?,
        name: String) : RelationshipGuard<K, V, FK, LinkedEvent<K, V, FK>>(foreignKeySelector, name) {

    override fun mapRelationship(event: Event<K, V>, foreignKey: FK): LinkedEvent<K, V, FK> {
        return LinkedEvent(event.key, event.version, event.valueClass, foreignKey)
    }
}