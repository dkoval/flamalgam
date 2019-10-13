package com.github.dkoval.core.dsl

interface Result<out PV> {
    val parent: PV
    operator fun <CK, CV> get(relationship: Relationship.OneToMany<CK, CV>): Iterable<CV>?
    operator fun <CK, CV> get(relationship: Relationship.ManyToOne<CK, CV>): CV?

    @Throws(NoSuchElementException::class)
    fun <CK, CV> getOrThrow(relationship: Relationship.OneToMany<CK, CV>): Iterable<CV> {
        return this[relationship] ?: throw NoSuchElementException("Result doesn't contain value for $relationship")
    }

    @Throws(NoSuchElementException::class)
    fun <CK, CV> getOrThrow(relationship: Relationship.ManyToOne<CK, CV>): CV {
        return this[relationship] ?: throw NoSuchElementException("Result doesn't contain value for $relationship")
    }
}