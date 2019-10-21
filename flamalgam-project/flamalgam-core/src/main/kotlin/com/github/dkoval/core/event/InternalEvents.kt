package com.github.dkoval.core.event

sealed class InternalEvent<out K : Any, out V : Any> : Event<K, V>

data class RelationshipDiscardedEvent<out K : Any, out V : Any>(
        override val key: K,
        override val version: Long,
        override val valueClass: Class<out V>) : InternalEvent<K, V>(), NoValueEvent<K, V>

fun <K : Any, V : Any> LifecycleEvent<K, V>.discardRelationship(): RelationshipDiscardedEvent<K, V> =
        RelationshipDiscardedEvent(key, version, valueClass)

data class LinkedEvent<out K : Any, out V : Any, out FK : Any>(
        override val key: K,
        override val version: Long,
        override val valueClass: Class<out V>,
        val foreignKey: FK) : InternalEvent<K, V>(), NoValueEvent<K, V>

data class RekeyedEvent<out K : Any>(
        override val key: K,
        val source: Event<*, *>,
        val isParent: Boolean = false) : InternalEvent<K, Any>() {

    override val version: Long
        get() = source.version

    override val value: Any?
        get() = source.value

    override val valueClass: Class<out Any>
        get() = source.valueClass
}

fun <K : Any> Event<*, *>.rekey(key: K, asParent: Boolean = false): RekeyedEvent<K> =
        RekeyedEvent(key, this, asParent)