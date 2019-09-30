package com.github.dkoval.core

interface Event<K : Comparable<K>> {
    val key: K
    val version: Long
    val type: Type

    enum class Type {
        CREATED, UPDATED, DELETED, RELATIONSHIP_DISCARDED
    }
}

data class ChildEvent<K : Comparable<K>>(
        override val key: K,
        override val version: Long,
        override val type: Event.Type,
        val foreignKey: K) : Event<K> {

    constructor(event: Event<K>, foreignKey: K) : this(event.key, event.version, event.type, foreignKey)
}

data class KeyedEvent<K : Comparable<K>>(
        override val key: K,
        private val event: Event<K>,
        val isParent: Boolean = false) : Event<K> {

    override val version: Long
        get() = event.version

    override val type: Event.Type
        get() = event.type
}

fun <K : Comparable<K>> Event<K>.isNewerThan(that: Event<K>): Boolean {
    if (this === that) {
        return false
    }
    val comparator = compareBy<Event<K>> { it.version }
    return comparator.compare(this, that) > 0
}