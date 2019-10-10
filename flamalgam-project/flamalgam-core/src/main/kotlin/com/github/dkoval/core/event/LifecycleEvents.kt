package com.github.dkoval.core.event

sealed class LifecycleEvent<out K, out V> : Event<K, V>

data class UpsertEvent<out K, out V>(
        override val key: K,
        override val version: Long,
        override val value: V) : LifecycleEvent<K, V>()

data class DeleteEvent<out K, V>(
        override val key: K,
        override val version: Long) : LifecycleEvent<K, V>() {

    override val value: V?
        get() = null
}