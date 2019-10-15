package com.github.dkoval.core.event

interface Event<out K : Any, out V : Any> {
    val key: K
    val version: Long
    val value: V?
    val valueClass: Class<out V>
}

fun <K : Any, V : Any> Event<K, V>.isNewerThan(that: Event<K, V>): Boolean {
    return if (this === that) false else compareBy<Event<K, V>> { it.version }.compare(this, that) > 0
}