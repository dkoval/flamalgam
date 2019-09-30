package com.github.dkoval.core

/**
 * A key/value pair to be passed through a data pipeline.
 *
 * @param K type of the key.
 * @param T type of the value.
 */
interface Record<K : Comparable<K>, out T>: Event<K> {
    val value: T
}