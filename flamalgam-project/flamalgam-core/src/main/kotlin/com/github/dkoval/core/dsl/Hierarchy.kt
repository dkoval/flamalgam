package com.github.dkoval.core.dsl

interface Hierarchy<out T : Any> {
    val parent: T
    operator fun <U : Any> get(relationship: Relationship.OneToMany<U>): Iterable<U>?
    operator fun <U : Any> get(relationship: Relationship.ManyToOne<U>): U?

    @Throws(NoSuchElementException::class)
    fun <U : Any> getOrThrow(relationship: Relationship.OneToMany<U>): Iterable<U> =
            this[relationship] ?: throw NoSuchElementException("Result doesn't contain value for $relationship")

    @Throws(NoSuchElementException::class)
    fun <U : Any> getOrThrow(relationship: Relationship.ManyToOne<U>): U =
            this[relationship] ?: throw NoSuchElementException("Result doesn't contain value for $relationship")
}