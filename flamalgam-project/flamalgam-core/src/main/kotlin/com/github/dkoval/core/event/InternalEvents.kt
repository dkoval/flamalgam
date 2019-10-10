package com.github.dkoval.core.event

sealed class InternalEvent<out K, out V> : Event<K, V>

data class RelationshipDiscardedEvent<out K, out PK>(
        override val key: K,
        override val version: Long,
        val oldParentKey: PK) : InternalEvent<K, Nothing>() {

    override val value: Nothing
        get() = throw IllegalStateException("No value is expected here")
}

data class RekeyedEvent<out K>(
        override val key: K,
        val source: Event<*, *>,
        val isParent: Boolean = false) : InternalEvent<K, Any?>() {

    override val version: Long
        get() = source.version

    override val value: Any?
        get() = source.value
}

fun <K, V> Event<*, V>.rekey(key: K, asParent: Boolean = false): RekeyedEvent<K> {
    return RekeyedEvent(key, this, asParent);
}