package com.github.dkoval.core.dsl

interface JoinResult<T : Any> {
    val parent: T
    operator fun <U : Any> get(relationship: Relationship.One<U>): U?
    operator fun <U : Any> get(relationship: Relationship.Many<U>): Iterable<U>?

    @Throws(NoSuchElementException::class)
    fun <U : Any> getOrThrow(relationship: Relationship.One<U>): U =
            this[relationship] ?: throw NoSuchElementException("Result doesn't contain value for $relationship")

    @Throws(NoSuchElementException::class)
    fun <U : Any> getOrThrow(relationship: Relationship.Many<U>): Iterable<U> =
            this[relationship] ?: throw NoSuchElementException("Result doesn't contain value for $relationship")
}