package com.github.dkoval.core.dsl.internal

import com.github.dkoval.core.dsl.Result
import com.github.dkoval.core.event.Event
import com.github.dkoval.core.event.RekeyedEvent
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.util.Collector

class StatefulResultMapper<K : Any, V : Any, R>(
        mapper: (Result<V>) -> Event<K, R>): RichFlatMapFunction<RekeyedEvent<K>, Event<K, R>>() {

    private lateinit var result: Result<V>

    override fun open(parameters: Configuration?) {
        result = StatefulResult()
    }

    override fun flatMap(value: RekeyedEvent<K>?, out: Collector<Event<K, R>>?) {
        TODO("not implemented")
    }
}