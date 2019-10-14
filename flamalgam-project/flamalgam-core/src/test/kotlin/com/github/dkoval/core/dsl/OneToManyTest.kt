package com.github.dkoval.core.dsl

import com.github.dkoval.core.event.*
import io.flinkspector.core.collection.ExpectedRecords
import io.flinkspector.core.input.InputBuilder
import io.flinkspector.datastream.DataStreamTestBase
import org.apache.flink.streaming.api.datastream.DataStream
import org.junit.Ignore
import org.junit.Test

class OneToManyTest : DataStreamTestBase() {

    data class Order(
            val id: Long)

    data class LineItem(
            val id: Long,
            val orderId: Long,
            val product: String,
            val quantity: Int = 1)

    private fun join(input: InputBuilder<Event<Long, Any>>): DataStream<RekeyedEvent<Long>> {
        val events: DataStream<Event<Long, Any>> = createTestStream(input)
        val orders = events.filterIsInstance<Long, Order>()
        val lineItems = events.filterIsInstance<Long, LineItem>()

        return Relationships.withParent(orders)
                .oneToMany(lineItems, { it.orderId }, Relationship.oneToMany("LineItem"))
                .join()
    }

    @Test
    fun `should create Order with 2 LineItems`() {
        val input = InputBuilder<Event<Long, Any>>()
                .emit(UpsertEvent(1L, 1L, Order(1L)))
                .emit(UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1")))
                .emit(UpsertEvent(2L, 1L, LineItem(2L, 1L, "Boots v1")))

        val result = join(input)

        val matcher = ExpectedRecords
                .create(RekeyedEvent(1L, UpsertEvent(1L, 1L, Order(1L)), isParent = true))
                .expect(RekeyedEvent(1L, UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1"))))
                .expect(RekeyedEvent(1L, UpsertEvent(2L, 1L, LineItem(2L, 1L, "Boots v1"))))

        assertStream(result, matcher)
    }

    @Test
    fun `should create Order and then handle LineItem updates`() {
        val input = InputBuilder<Event<Long, Any>>()
                .emit(UpsertEvent(1L, 1L, Order(1L)))
                .emit(UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1")))
                .emit(UpsertEvent(1L, 2L, LineItem(1L, 1L, "T-shirt v2")))

        val result = join(input)

        val matcher = ExpectedRecords
                .create(RekeyedEvent(1L, UpsertEvent(1L, 1L, Order(1L)), isParent = true))
                .expect(RekeyedEvent(1L, UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1"))))
                .expect(RekeyedEvent(1L, UpsertEvent(1L, 2L, LineItem(1L, 1L, "T-shirt v2"))))

        assertStream(result, matcher)
    }

    @Test
    fun `should create Order and then handle stale LineItem updates`() {
        val input = InputBuilder<Event<Long, Any>>()
                .emit(UpsertEvent(1L, 1L, Order(1L)))
                .emit(UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1")))
                .emit(UpsertEvent(1L, 3L, LineItem(1L, 1L, "T-shirt v3")))
                .emit(UpsertEvent(1L, 2L, LineItem(1L, 1L, "T-shirt v2")))

        val result = join(input)

        // assert that out-of-order LineItem updates are handled properly, that is, an older update v2 gets ignored
        val matcher = ExpectedRecords
                .create(RekeyedEvent(1L, UpsertEvent(1L, 1L, Order(1L)), isParent = true))
                .expect(RekeyedEvent(1L, UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1"))))
                .expect(RekeyedEvent(1L, UpsertEvent(1L, 3L, LineItem(1L, 1L, "T-shirt v3"))))

        assertStream(result, matcher)
    }

    @Ignore // currently fails due to org.apache.flink.streaming.api.functions.source.FromElementsFunction.checkCollection(FromElementsFunction.java:228)
    @Test
    fun `should create Order and then handle LineItem deletes`() {
        val input = InputBuilder<Event<Long, Any>>()
                .emit(UpsertEvent(1L, 1L, Order(1L)))
                .emit(UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1")))
                .emit(DeleteEvent<Long, LineItem>(1L, 2L))

        val result = join(input)

        val matcher = ExpectedRecords
                .create(RekeyedEvent(1L, UpsertEvent(1L, 1L, Order(1L)), isParent = true))
                .expect(RekeyedEvent(1L, UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1"))))
                .expect(RekeyedEvent(1L, DeleteEvent<Long, LineItem>(1L, 2L)))

        assertStream(result, matcher)
    }
}