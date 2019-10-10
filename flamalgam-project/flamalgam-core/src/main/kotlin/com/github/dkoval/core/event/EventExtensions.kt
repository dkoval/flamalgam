package com.github.dkoval.core.event

import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.util.Collector

@Suppress("UNCHECKED_CAST")
inline fun <K, reified R> DataStream<Event<K, Any>>.filterIsInstance(): DataStream<Event<K, R>> {
    return this
            .flatMap { event: Event<K, Any>, out: Collector<Event<K, R>> ->
                val value = event.value
                if (value is R) {
                    out.collect(event as Event<K, R>)
                }
            }
            .returns(object : TypeHint<Event<K, R>>() {})
            .name("DataStream[${R::class.java.simpleName}]")
}