package com.github.dkoval.core.dsl

interface JoinResult<T : Any> {
    val parent: T
    operator fun <U : Any> get(cardinality: Cardinality.One<U>): U?
    operator fun <U : Any> get(cardinality: Cardinality.Many<U>): Iterable<U>?

    @Throws(NoSuchElementException::class)
    fun <U : Any> getChild(cardinality: Cardinality.One<U>): U =
            this[cardinality] ?: throw NoSuchElementException("Result doesn't contain value for $cardinality")

    @Throws(NoSuchElementException::class)
    fun <U : Any> getChild(cardinality: Cardinality.Many<U>): Iterable<U> =
            this[cardinality] ?: throw NoSuchElementException("Result doesn't contain value for $cardinality")

    @JvmDefault
    fun <R> map(block: (JoinResult<T>) -> R) = let(block)
}