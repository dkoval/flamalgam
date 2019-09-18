package com.github.dkoval.core.dsl

import com.github.dkoval.core.Record
import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.streaming.api.datastream.DataStream

class EstablishedRelations<K : Comparable<K>, V : Any>(
        private val parent: Relations<K, V>) : Relations<K, V>(parent) {

    override fun <U : Any> join(stream: DataStream<Record<K, U>>,
                                relationship: Relationship<U>): EstablishedRelations<K, V> {
        // TODO: implement
        return this
    }

    fun done(): DataStream<JoinResult<V>> = TODO()

    fun <R : Any> map(mapper: MapFunction<JoinResult<V>, R>) = done().map(mapper)
}