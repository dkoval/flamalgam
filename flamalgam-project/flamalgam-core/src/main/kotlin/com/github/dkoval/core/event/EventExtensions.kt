package com.github.dkoval.core.event

import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.util.Collector

@Suppress("UNCHECKED_CAST")
inline fun <K : Any, reified R : Any> DataStream<LifecycleEvent<K, Any>>.filterIsInstance(): DataStream<LifecycleEvent<K, R>> = this
        .flatMap { event: LifecycleEvent<K, Any>, out: Collector<LifecycleEvent<K, R>> ->
            if (R::class.java.isAssignableFrom(event.valueClass)) {
                out.collect(event as LifecycleEvent<K, R>)
            }
        }
        .returns(object : TypeHint<LifecycleEvent<K, R>>() {})