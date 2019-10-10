package com.github.dkoval.core.event

interface Event<out K, out V> {
    val key: K
    val version: Long
    val value: V?
}

fun <K, V> Event<K, V>.isNewerThan(that: Event<K, V>): Boolean {
    if (this === that) {
        return false
    }
    val comparator = compareBy<Event<K, V>> { it.version }
    return comparator.compare(this, that) > 0
}