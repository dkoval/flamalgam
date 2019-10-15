package com.github.dkoval.core.event

sealed class InternalEvent<out K : Any, out V : Any> : Event<K, V>

data class RelationshipDiscardedEvent<out CK : Any, out CV : Any, out PK : Any>(
        override val key: CK,
        override val version: Long,
        override val valueClass: Class<out CV>,
        val oldParentKey: PK) : InternalEvent<CK, CV>() {

    override val value: Nothing
        get() = throw IllegalStateException("No value is expected here")
}

fun <CK : Any, CV : Any, PK : Any> Event<CK, CV>.discardRelationship(oldParentKey: PK): RelationshipDiscardedEvent<CK, CV, PK> =
        RelationshipDiscardedEvent(key, version, valueClass, oldParentKey)

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