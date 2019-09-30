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

internal sealed class RelationshipGuard<K : Comparable<K>, T : Any>(
        private val foreignKeySelector: KeySelector<T, K>,
        name: String) : RichFlatMapFunction<Record<K, T>, KeyedEvent<K>>() {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val name = "$name-${javaClass.simpleName}"
    private lateinit var relationshipState: ValueState<ChildEvent<K>>

    override fun open(parameters: Configuration) {
        relationshipState = runtimeContext.getState(
                ValueStateDescriptor(name, TypeInformation.of(object : TypeHint<ChildEvent<K>>() {})))
    }

    override fun flatMap(newRecord: Record<K, T>, out: Collector<KeyedEvent<K>>) {
        val value = newRecord.value
        val newForeignKey = foreignKeySelector.getKey(value)

        val curRecord = relationshipState.value()
        if (curRecord != null && !newRecord.isNewerThan(curRecord)) {
            logger.debug("Ignoring stale record. New record: {}, current record: {}", newRecord, curRecord)
            return
        }

        // update the state of relationship
        relationshipState.update(ChildEvent(newRecord, newForeignKey))

        // handle relationship change, if applicable
        val curForeignKey = curRecord?.foreignKey
        if (curForeignKey != null && (newForeignKey == null || curForeignKey != newForeignKey)) {
            // emit `relationship discarded` event
            val decor = ChildEvent(newRecord.key, newRecord.version, Event.Type.RELATIONSHIP_DISCARDED, curForeignKey)
            val event = mapRelationship(decor, curForeignKey)
            out.collect(event)
        }

        // emit regular event
        if (newForeignKey != null) {
            val event = mapRelationship(newRecord, newForeignKey)
            out.collect(event)
        }
    }

    private fun mapRelationship(newEvent: Event<K>, foreignKey: K): KeyedEvent<K> {
        return KeyedEvent(foreignKey, newEvent)
    }

    companion object {
        @JvmStatic
        fun <K : Comparable<K>, T : Any> oneToMany(foreignKeySelector: KeySelector<T, K>,
                                                   name: String): RelationshipGuard<K, T> {
            return OneToManyRelationshipGuard(foreignKeySelector, name)
        }
    }
}

private class OneToManyRelationshipGuard<K : Comparable<K>, T : Any>(
        foreignKeySelector: KeySelector<T, K>,
        name: String) : RelationshipGuard<K, T>(foreignKeySelector, name)