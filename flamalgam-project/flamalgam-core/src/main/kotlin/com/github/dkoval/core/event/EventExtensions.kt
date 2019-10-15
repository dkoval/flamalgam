package com.github.dkoval.core.event

import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.util.Collector

@Suppress("UNCHECKED_CAST")
inline fun <K : Any, reified R : Any> DataStream<Event<K, Any>>.filterIsInstance(): DataStream<Event<K, R>> = this
        .flatMap { event: Event<K, Any>, out: Collector<Event<K, R>> ->
            if (R::class.java.isAssignableFrom(event.valueClass)) {
                out.collect(event as Event<K, R>)
            }
        }
        .returns(object : TypeHint<Event<K, R>>() {})