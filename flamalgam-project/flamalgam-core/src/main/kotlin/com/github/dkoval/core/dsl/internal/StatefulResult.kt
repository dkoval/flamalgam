package com.github.dkoval.core.dsl.internal

import com.github.dkoval.core.dsl.Relationship
import com.github.dkoval.core.dsl.Result

class StatefulResult<T : Any> : Result<T> {

    override val parent: T
        get() = TODO("not implemented")

    override fun <U : Any> get(relationship: Relationship.OneToMany<U>): Iterable<U>? {
        TODO("not implemented")
    }

    override fun <U : Any> get(relationship: Relationship.ManyToOne<U>): U? {
        TODO("not implemented")
    }
}