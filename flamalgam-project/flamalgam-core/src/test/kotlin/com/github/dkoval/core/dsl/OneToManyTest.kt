package com.github.dkoval.core.dsl

import com.github.dkoval.core.event.Event
import com.github.dkoval.core.event.RekeyedEvent
import com.github.dkoval.core.event.UpsertEvent
import com.github.dkoval.core.event.filterIsInstance
import io.flinkspector.core.collection.ExpectedRecords
import io.flinkspector.core.input.InputBuilder
import io.flinkspector.datastream.DataStreamTestBase
import org.apache.flink.api.java.functions.KeySelector
import org.apache.flink.streaming.api.datastream.DataStream
import org.junit.Test

class OneToManyTest : DataStreamTestBase() {

    data class Order(
            val id: Int)

    data class LineItem(
            val id: Int,
            val orderId: Int,
            val product: String,
            val quantity: Int = 1)

    @Test
    fun `happy path`() {
        val input = InputBuilder<Event<Int, Any>>()
                .emit(UpsertEvent(1, 1L, Order(1)))
                .emit(UpsertEvent(1, 1L, LineItem(1, 1, "T-shirt")))
                .emit(UpsertEvent(2, 1L, LineItem(2, 1, "Boots")))

        val events: DataStream<Event<Int, Any>> = createTestStream(input)
        val orders = events.filterIsInstance<Int, Order>()
        val lineItems = events.filterIsInstance<Int, LineItem>()

        val result = Relationships.parent(orders)
                .oneToMany(lineItems, KeySelector { it.orderId }, Relationship.oneToMany("LineItem"))
                .join()

        val matcher =  ExpectedRecords
                .create(RekeyedEvent(1, UpsertEvent(1, 1L, Order(1)), true))
                .expect(RekeyedEvent(1, UpsertEvent(1, 1L, LineItem(1, 1, "T-shirt"))))
                .expect(RekeyedEvent(1, UpsertEvent(2, 1L, LineItem(2, 1, "Boots"))))

        assertStream(result, matcher)
    }
}