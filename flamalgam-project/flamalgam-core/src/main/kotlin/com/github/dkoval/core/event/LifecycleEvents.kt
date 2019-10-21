package com.github.dkoval.core.event

sealed class LifecycleEvent<out K : Any, out V : Any> : Event<K, V> {

    abstract fun withoutValue(): NoValueEvent<K, V>
}

data class UpsertEvent<out K : Any, out V : Any>(
        override val key: K,
        override val version: Long,
        override val value: V) : LifecycleEvent<K, V>() {

    override val valueClass: Class<out V>
        get() = value.javaClass

    override fun withoutValue(): NoValueEvent<K, V> = DefaultNoValueEvent(key, version, valueClass)
}

data class DeleteEvent<out K : Any, out V : Any>(
        override val key: K,
        override val version: Long,
        override val valueClass: Class<out V>) : LifecycleEvent<K, V>(), NoValueEvent<K, V> {

    override fun withoutValue(): NoValueEvent<K, V> = this
}