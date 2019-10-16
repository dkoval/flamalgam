package com.github.dkoval.core.dsl

import com.github.dkoval.core.event.*
import io.flinkspector.core.collection.ExpectedRecords
import io.flinkspector.core.input.InputBuilder
import io.flinkspector.core.input.InputTranslator
import io.flinkspector.datastream.DataStreamTestBase
import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.api.java.tuple.Tuple1
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

    private fun runTestCase(input: InputBuilder<Event<Long, Any>>): DataStream<RekeyedEvent<Long>> {
        val events: DataStream<Event<Long, Any>> = doCreateTestStream(input)
        val orders = events.filterIsInstance<Long, Order>()
        val lineItems = events.filterIsInstance<Long, LineItem>()

        return Relationships.withParent(orders)
                .oneToMany(lineItems, { it.orderId }, Cardinality.many("LineItem"))
                .join()
    }

    private fun <K : Any> doCreateTestStream(input: InputBuilder<Event<K, Any>>): DataStream<Event<K, Any>> {
        // Wrapping/unwrapping Event<K, Any> to/from Tuple1<Event<K, Any>> is sort of a hack to allow
        // different types of events to be added to the same DataStream instance. The trick is meant to workaround
        // >> org.apache.flink.streaming.api.functions.source.FromElementsFunction.checkCollection(FromElementsFunction.java)
        // check in Apache Flink.
        val translatedInput = object : InputTranslator<Event<K, Any>, Tuple1<Event<K, Any>>>(input) {
            override fun translate(elem: Event<K, Any>): Tuple1<Event<K, Any>> {
                return Tuple1.of(elem)
            }
        }
        return createTestStream(translatedInput)
                .map { it.f0 }
                .returns(object : TypeHint<Event<K, Any>>() {})
    }

    @Test
    fun `should create Order with 2 LineItems`() {
        val input = InputBuilder<Event<Long, Any>>()
                .emit(UpsertEvent(1L, 1L, Order(1L)))
                .emit(UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1")))
                .emit(UpsertEvent(2L, 1L, LineItem(2L, 1L, "Boots v1")))

        val result = runTestCase(input)

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

        val result = runTestCase(input)

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
                // LineItem update v2 comes out-of-order
                .emit(UpsertEvent(1L, 2L, LineItem(1L, 1L, "T-shirt v2")))

        val result = runTestCase(input)

        // assert out-of-order LineItem updates are handled properly, i.e. an older update v2 gets ignored
        val matcher = ExpectedRecords
                .create(RekeyedEvent(1L, UpsertEvent(1L, 1L, Order(1L)), isParent = true))
                .expect(RekeyedEvent(1L, UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1"))))
                .expect(RekeyedEvent(1L, UpsertEvent(1L, 3L, LineItem(1L, 1L, "T-shirt v3"))))

        assertStream(result, matcher)
    }

    @Test
    fun `should create Order and then handle LineItem deletes`() {
        val input = InputBuilder<Event<Long, Any>>()
                .emit(UpsertEvent(1L, 1L, Order(1L)))
                .emit(UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1")))
                .emit(DeleteEvent(1L, 2L, LineItem::class.java))

        val result = runTestCase(input)

        val matcher = ExpectedRecords
                .create(RekeyedEvent(1L, UpsertEvent(1L, 1L, Order(1L)), isParent = true))
                .expect(RekeyedEvent(1L, UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1"))))
                .expect(RekeyedEvent(1L, DeleteEvent(1L, 2L, LineItem::class.java)))

        assertStream(result, matcher)
    }

    @Test
    fun `should create 2 Orders and then handle relationship changes between LineItem and Order`() {
        val input = InputBuilder<Event<Long, Any>>()
                .emit(UpsertEvent(1L, 1L, Order(1L)))
                .emit(UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1")))
                .emit(UpsertEvent(2L, 1L, Order(2L)))
                .emit(UpsertEvent(2L, 1L, LineItem(2L, 2L, "Boots v1")))
                // now LineItem(id=2) gets re-attached to Order(id=1), previous association gets discarded
                .emit(UpsertEvent(2L, 2L, LineItem(2L, 1L, "Boots v2")))

        val result = runTestCase(input)

        val matcher = ExpectedRecords
                .create(RekeyedEvent(1L, UpsertEvent(1L, 1L, Order(1L)), isParent = true))
                .expect(RekeyedEvent(1L, UpsertEvent(1L, 1L, LineItem(1L, 1L, "T-shirt v1"))))
                .expect(RekeyedEvent(2L, UpsertEvent(2L, 1L, Order(2L)), isParent = true))
                .expect(RekeyedEvent(2L, UpsertEvent(2L, 1L, LineItem(2L, 2L, "Boots v1"))))
                .expect(RekeyedEvent(2L, RelationshipDiscardedEvent(2L, 2L, LineItem::class.java, 2L)))
                .expect(RekeyedEvent(1L, UpsertEvent(2L, 2L, LineItem(2L, 1L, "Boots v2"))))

        assertStream(result, matcher)
    }
}