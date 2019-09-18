package com.github.dkoval.core.dsl

interface JoinResult<T : Any> {
    val parent: T
    operator fun <U : Any> get(relationship: Relationship.One<U>): U?
    operator fun <U : Any> get(relationship: Relationship.Many<U>): Iterable<U>?

    @Throws(NoSuchElementException::class)
    fun <U : Any> getChild(relationship: Relationship.One<U>): U =
            this[relationship] ?: throw NoSuchElementException("Result doesn't contain value for $relationship")

    @Throws(NoSuchElementException::class)
    fun <U : Any> getChildren(relationship: Relationship.Many<U>): Iterable<U> =
            this[relationship] ?: throw NoSuchElementException("Result doesn't contain value for $relationship")

    @JvmDefault
    fun <R> map(block: (JoinResult<T>) -> R) = let(block)
}