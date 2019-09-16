package com.github.dkoval.core

/**
 * A key/value pair to be passed through a data pipeline.
 *
 * @param K type of the key.
 * @param V type of the value.
 */
interface Record<K : Comparable<K>, V : Any> {
    val key: K
    val value: V
    val version: Long
    val type: Type

    enum class Type {
        CREATED, UPDATED, DELETED
    }
}