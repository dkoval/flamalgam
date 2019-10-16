package com.github.dkoval.core.dsl

interface Result<out PV> {
    val parent: PV
    operator fun <CK, CV> get(cardinality: Cardinality.Many<CK, CV>): Iterable<CV>?
    operator fun <CK, CV> get(cardinality: Cardinality.One<CK, CV>): CV?

    @Throws(NoSuchElementException::class)
    fun <CK, CV> getOrThrow(cardinality: Cardinality.Many<CK, CV>): Iterable<CV> {
        return this[cardinality] ?: throw NoSuchElementException("Result doesn't contain value for $cardinality")
    }

    @Throws(NoSuchElementException::class)
    fun <CK, CV> getOrThrow(cardinality: Cardinality.One<CK, CV>): CV {
        return this[cardinality] ?: throw NoSuchElementException("Result doesn't contain value for $cardinality")
    }
}