package com.github.dkoval.core.dsl

interface Result<out PV : Any> {
    val parent: PV
    operator fun <CK : Any, CV : Any> get(lookupKey: LookupKey.One<CK, CV>): CV?
    operator fun <CK : Any, CV : Any> get(lookupKey: LookupKey.Many<CK, CV>): Iterable<CV>?

    @Throws(NoSuchElementException::class)
    fun <CK : Any, CV : Any> getOrThrow(lookupKey: LookupKey.One<CK, CV>): CV {
        return this[lookupKey] ?: throw NoSuchElementException("Result doesn't contain value for $lookupKey")
    }

    @Throws(NoSuchElementException::class)
    fun <CK : Any, CV : Any> getOrThrow(lookupKey: LookupKey.Many<CK, CV>): Iterable<CV> {
        return this[lookupKey] ?: throw NoSuchElementException("Result doesn't contain value for $lookupKey")
    }
}

sealed class LookupKey<K : Any, V : Any> {
    abstract val name: String
    abstract val eventKeyClass: Class<K>
    abstract val eventValueClass: Class<V>

    data class One<K : Any, V : Any>(
            override val name: String,
            override val eventKeyClass: Class<K>,
            override val eventValueClass: Class<V>) : LookupKey<K, V>()

    data class Many<K : Any, V : Any>(
            override val name: String,
            override val eventKeyClass: Class<K>,
            override val eventValueClass: Class<V>) : LookupKey<K, V>()

    companion object {
        @JvmStatic
        inline fun <reified K : Any, reified T : Any> one(name: String) = One(name, K::class.java, T::class.java)

        @JvmStatic
        inline fun <reified K : Any, reified V : Any> many(name: String) = Many(name, K::class.java, V::class.java)
    }
}