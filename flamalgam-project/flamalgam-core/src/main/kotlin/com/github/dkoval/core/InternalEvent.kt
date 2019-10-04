package com.github.dkoval.core

sealed class InternalEvent<out K : Any, out V> : Event<K, V>

data class RelationshipDiscardedEvent<out K : Any, out PK : Any>(
        override val key: K,
        override val version: Long,
        val oldParentKey: PK) : InternalEvent<K, Nothing>() {

    override val value: Nothing
        get() = throw IllegalStateException("No value is expected here")
}

data class RekeyedEvent<out K : Any>(
        override val key: K,
        val source: Event<*, *>,
        val isParent: Boolean = false) : InternalEvent<K, Any?>() {

    override val version: Long
        get() = source.version

    override val value: Any?
        get() = source.value
}

fun <K : Any, V> Event<*, V>.rekey(key: K, asParent: Boolean = false): RekeyedEvent<K> {
    return RekeyedEvent(key, this, asParent);
}