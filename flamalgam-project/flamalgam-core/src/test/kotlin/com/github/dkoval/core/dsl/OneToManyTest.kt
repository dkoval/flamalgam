package com.github.dkoval.core.dsl

import com.github.dkoval.core.event.Event
import com.github.dkoval.core.event.RekeyedEvent
import com.github.dkoval.core.event.UpsertEvent
import com.github.dkoval.core.event.filterIsInstance
import io.flinkspector.core.collection.ExpectedRecords
import io.flinkspector.core.input.InputBuilder
import io.flinkspector.datastream.DataStreamTestBase
import org.apache.flink.streaming.api.datastream.DataStream
import org.junit.Test

class OneToManyTest : DataStreamTestBase() {

    data class Order(
            val id: Long)

    data class LineItem(
            val id: Long,
            val orderId: Long,
            val product: String,
            val quantity: Int = 1)

    @Test
    fun `happy path`() {
        val input = InputBuilder<Event<Long, Any>>()
                .emit(UpsertEvent(1L, 1L, Order(1L)))
                .emit(UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt")))
                .emit(UpsertEvent(2L, 1L, LineItem(2L, 1L, "Boots")))

        val events: DataStream<Event<Long, Any>> = createTestStream(input)
        val orders = events.filterIsInstance<Long, Order>()
        val lineItems = events.filterIsInstance<Long, LineItem>()

        val result = Relationships.withParent(orders)
                .oneToMany(lineItems, { it.orderId }, Relationship.oneToMany("LineItem"))
                .join()

        val matcher = ExpectedRecords
                .create(RekeyedEvent(1L, UpsertEvent(1L, 1L, Order(1L)), true))
                .expect(RekeyedEvent(1L, UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt"))))
                .expect(RekeyedEvent(1L, UpsertEvent(2L, 1L, LineItem(2L, 1L, "Boots"))))

        assertStream(result, matcher)
    }
}