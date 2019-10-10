package com.github.dkoval.core.dsl

interface Result<out T> {
    val parent: T
    operator fun <U> get(relationship: Relationship.OneToMany<U>): Iterable<U>?
    operator fun <U> get(relationship: Relationship.ManyToOne<U>): U?

    @Throws(NoSuchElementException::class)
    fun <U> getOrThrow(relationship: Relationship.OneToMany<U>): Iterable<U> =
            this[relationship] ?: throw NoSuchElementException("Result doesn't contain value for $relationship")

    @Throws(NoSuchElementException::class)
    fun <U> getOrThrow(relationship: Relationship.ManyToOne<U>): U =
            this[relationship] ?: throw NoSuchElementException("Result doesn't contain value for $relationship")
}