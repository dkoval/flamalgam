package com.github.dkoval.core.event

interface Event<out K : Any, out V : Any> {
    val key: K
    val version: Long
    val value: V?
    val valueClass: Class<out V>
}

interface NoValueEvent<out K : Any, out V : Any> : Event<K, V> {
    @JvmDefault
    override val value: Nothing
        get() = throw IllegalStateException("No value is expected here")
}

data class DefaultNoValueEvent<out K : Any, out V : Any>(
        override val key: K,
        override val version: Long,
        override val valueClass: Class<out V>) : NoValueEvent<K, V>

fun <K : Any, V : Any> Event<K, V>.isNewerThan(that: Event<K, V>): Boolean =
        if (this === that) false else compareBy<Event<K, V>> { it.version }.compare(this, that) > 0