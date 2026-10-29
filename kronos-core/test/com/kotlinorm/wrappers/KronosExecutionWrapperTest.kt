@file:OptIn(com.kotlinorm.annotations.InternalKronosApi::class)

package com.kotlinorm.wrappers

import com.kotlinorm.utils.execute
import com.kotlinorm.Kronos
import com.kotlinorm.beans.task.ResultColumnMetadata
import com.kotlinorm.beans.dsl.Field
import com.kotlinorm.beans.task.KronosAtomicActionTask
import com.kotlinorm.beans.task.KronosAtomicBatchTask
import com.kotlinorm.beans.task.KronosAtomicQueryTask
import com.kotlinorm.interfaces.*
import com.kotlinorm.exceptions.ConflictingResultColumnLabels
import com.kotlinorm.enums.DBType
import com.kotlinorm.enums.KOperationType
import kotlin.reflect.typeOf
import kotlin.test.*

class KronosExecutionWrapperTest {
    @Test
    fun expandsListParametersBeforeThePhysicalBoundary() {
        val source = RecordingSource()
        val wrapper = KronosExecutionWrapper(source)
        wrapper.toList(KronosAtomicQueryTask("SELECT id FROM record WHERE id IN (:ids)",
            mapOf("ids" to listOf(1, 2)), targetType = typeOf<Map<String, Any?>>(),
            listParameterOccurrences = setOf(0)))
        assertEquals("SELECT id FROM record WHERE id IN (?, ?)", source.query?.sql)
        assertEquals(listOf("__kronos_p0", "__kronos_p1"), source.query?.parameterOrder)
        assertEquals(mapOf("__kronos_p0" to 1, "__kronos_p1" to 2), source.query?.parameters)
    }

    @Test
    fun mapsRawMapsAndTypedScalarsWhileTheCursorIsLive() {
        val source = RecordingSource(listOf("id" to KronosPhysicalValue.Integer(7)))
        val wrapper = KronosExecutionWrapper(source)
        assertEquals(listOf(mapOf("id" to 7L)), wrapper.toList(KronosAtomicQueryTask(
            "SELECT id FROM record", targetType = typeOf<Map<String, Any?>>())))
        assertEquals(7, wrapper.first(KronosAtomicQueryTask("SELECT id FROM record", targetType = typeOf<Int>())))
        assertEquals(7L, wrapper.first(KronosAtomicQueryTask("SELECT id FROM record", targetType = typeOf<Any?>())))
    }

    @Test
    fun rowMappingPreservesNullPresenceAndRejectsEscapedRows() {
        val source = RecordingSource(listOf("id" to KronosPhysicalValue.Integer(7)))
        val wrapper = KronosExecutionWrapper(source)
        val task = KronosAtomicQueryTask("SELECT id FROM record", targetType = typeOf<Any?>())
        var escaped: KronosRow? = null
        val result = wrapper.first(task) { row -> escaped = row; null }
        assertEquals(KronosRowFirstResult.Present(null), result)
        assertFailsWith<IllegalStateException> { escaped!!.get<Int>(1) }
    }

    @Test
    fun duplicateLabelsRequireAnExplicitPhysicalPosition() {
        val source = RecordingSource(listOf("id" to KronosPhysicalValue.Integer(1), "id" to KronosPhysicalValue.Integer(2)))
        val wrapper = KronosExecutionWrapper(source)
        val task = KronosAtomicQueryTask("SELECT a.id, b.id FROM a, b", targetType = typeOf<Map<String, Any?>>())
        assertFailsWith<IllegalArgumentException> { wrapper.toList(task) }
        assertEquals(listOf(1 to 2), wrapper.toList(task) { it.get<Int>(1) to it.get<Int>(2) })
    }

    @Test
    fun copiesAtomicGeneratedKeysAndBatchResults() {
        val source = RecordingSource()
        val wrapper = KronosExecutionWrapper(source)
        val task = KronosAtomicActionTask("INSERT INTO record (name) VALUES (:name)", mapOf("name" to "Kronos"),
            KOperationType.INSERT, generatedKeyField = Field("id"))
        assertEquals(1, wrapper.update(task))
        assertEquals(listOf<Any?>(8L), task.generatedKeys)
        assertEquals(8L, task.lastInsertId)
        assertEquals(KronosGeneratedKeyRequest("id"), source.action?.generatedKey)
        val batch = KronosAtomicBatchTask("UPDATE record SET name=:name", arrayOf(mapOf("name" to "a"), mapOf("name" to "b")))
        assertContentEquals(intArrayOf(1, 1), wrapper.batchUpdate(batch))
        assertEquals(listOf(mapOf("__kronos_p0" to "a"), mapOf("__kronos_p0" to "b")), source.batch?.entries)
    }

    @Test
    fun generatedKeyRequestsUseThePhysicalColumnForSingleAndBatchWrites() {
        val source = RecordingSource()
        val wrapper = KronosExecutionWrapper(source)
        val identity = Field("person_key", "personKey")
        val single = KronosAtomicActionTask("INSERT INTO record(name) VALUES (:name)",
            mapOf("name" to "Ada"), KOperationType.INSERT, generatedKeyField = identity)
        assertEquals(1, wrapper.update(single))
        assertEquals(KronosGeneratedKeyRequest("person_key"), source.action?.generatedKey)
        val batch = KronosAtomicBatchTask("INSERT INTO record(name) VALUES (:name)",
            arrayOf(mapOf("name" to "Ada"), mapOf("name" to "Grace")),
            KOperationType.INSERT, generatedKeyField = identity)
        assertContentEquals(intArrayOf(1, 1), wrapper.batchUpdate(batch))
        assertEquals(KronosGeneratedKeyRequest("person_key"), source.batch?.generatedKey)
    }

    @Test
    fun transactionsForwardTimeoutAndOpaqueSavepointOperations() {
        val source = RecordingSource()
        val wrapper = KronosExecutionWrapper(source)
        val result = wrapper.transact(null, 4) {
            val checkpoint = savepoint("checkpoint")
            rollbackToSavepoint(checkpoint)
            releaseSavepoint(checkpoint)
            "result"
        }
        assertEquals("result", result)
        assertEquals(listOf(KronosTransactionOptions(timeoutSeconds = 4)), source.transactions)
        assertEquals(listOf("savepoint:checkpoint", "rollback", "release"), source.savepoints)
        assertFailsWith<IllegalArgumentException> { wrapper.transact(com.kotlinorm.enums.TransactionIsolation.READ_COMMITTED, null) {} }
    }

    @Test
    fun atomicEmptyKeyResultsNeverReadAnotherConnection() {
        listOf(0, 1).forEach { affectedRows ->
            val source = RecordingSource(actionResult = KronosExecutionResult(affectedRows))
            val wrapper = KronosExecutionWrapper(source)
            val statement = com.kotlinorm.syntax.statement.SqlDmlStatement.Insert(
                com.kotlinorm.syntax.table.SqlTable.Ident("record"),
                listOf(com.kotlinorm.syntax.SqlIdentifier.of("name")),
                com.kotlinorm.syntax.statement.SqlInsertMode.Values(listOf(listOf(com.kotlinorm.syntax.expr.SqlExpr.StringLiteral("Kronos")))))
            val task = KronosAtomicActionTask("INSERT INTO record (name) VALUES ('Kronos')",
                operationType = KOperationType.INSERT, statement = statement, generatedKeyField = Field("id"))
            val result = task.execute(wrapper)
            assertEquals(affectedRows, result.affectedRows)
            assertNull(result.lastInsertId)
            assertNull(source.query)
        }
    }

    @Test
    fun aliasedPojoColumnsUseDeclaredPropertyAndDecodeOnce() {
        val factory = Kronos.registerKPojoFactory(typeOf<AliasedRecord>()) { AliasedRecord() }
        var decodes = 0
        val codec = Kronos.registerValueCodec(valueCodec(
            supports = { _, context -> context.direction == com.kotlinorm.enums.ValueCodecDirection.DECODE && context.field?.name == "name" },
            convert = { value, _ -> decodes++; "decoded:$value" }
        ))
        try {
            val field = AliasedRecord().__columns.first()
            val task = KronosAtomicQueryTask("SELECT full_name AS display_name FROM record",
                targetType = typeOf<AliasedRecord>(),
                resultColumns = mapOf("display_name" to ResultColumnMetadata(typeOf<String?>(), field)))
            val source = RecordingSource(listOf("display_name" to KronosPhysicalValue.Text("Kronos")))
            val record = KronosExecutionWrapper(source).first(task) as AliasedRecord
            assertEquals("decoded:Kronos", record.name)
            assertEquals(1, decodes)
        } finally { codec.close(); factory.close() }
    }

    @Test
    fun outputPropertyNamesTakePriorityOverSourceFieldNames() {
        val factory = Kronos.registerKPojoFactory(typeOf<AliasedRecord>()) { AliasedRecord() }
        try {
            val sourceField = AliasedRecord().__columns.first()
            val task = KronosAtomicQueryTask("SELECT a.name, b.name AS name_1 FROM a, b",
                targetType = typeOf<AliasedRecord>(), resultColumns = mapOf(
                    "name" to ResultColumnMetadata(typeOf<String?>(), sourceField),
                    "name_1" to ResultColumnMetadata(typeOf<String?>(), sourceField)))
            val source = RecordingSource(listOf("name" to KronosPhysicalValue.Text("first"), "name_1" to KronosPhysicalValue.Text("second")))
            val record = KronosExecutionWrapper(source).first(task) as AliasedRecord
            assertEquals("first", record.name)
            assertEquals("second", record.name_1)
        } finally { factory.close() }
    }

    @Test
    fun reusedPojoFactoryInstancesAreRejectedWithinOneResult() {
        val shared = AliasedRecord()
        val factory = Kronos.registerKPojoFactory(typeOf<AliasedRecord>()) { shared }
        try {
            val source = RecordingSource(listOf("full_name" to KronosPhysicalValue.Text("Kronos")), rowCount = 2)
            val task = KronosAtomicQueryTask("SELECT full_name FROM record", targetType = typeOf<AliasedRecord>())
            assertFailsWith<IllegalStateException> { KronosExecutionWrapper(source).toList(task) }
        } finally { factory.close() }
    }

    @Test
    fun emptyQueriesAndIdentityFallbacksKeepTheirPublicContract() {
        val source = RecordingSource()
        val wrapper = KronosExecutionWrapper(source)
        assertEquals(":memory:", wrapper.url)
        assertEquals("", wrapper.userName)
        assertEquals(DBType.SQLite, wrapper.dbType)
        val task = KronosAtomicQueryTask("SELECT id", targetType = typeOf<Int>())
        assertNull(wrapper.first(task))
        assertEquals(KronosRowFirstResult.Empty, wrapper.first(task) { it.get<Int>(1) })
        val explicit = KronosExecutionWrapper(RecordingSource(identity = KronosDatabaseIdentity(
            DBType.SQLite, "native-name", url = "jdbc:sqlite:actual", userName = "reader")))
        assertEquals("jdbc:sqlite:actual", explicit.url)
        assertEquals("reader", explicit.userName)
    }

    @Test
    fun typedMapsOverrideStaleMetadataAndRawMapsPreserveStorageValues() {
        val columns = listOf("COUNT" to KronosPhysicalValue.Text("42"))
        val source = RecordingSource(columns)
        val stale = ResultColumnMetadata(typeOf<String>(), typeInfo = com.kotlinorm.utils.StaticTypeInfo(
            com.kotlinorm.utils.StaticTypeRef("kotlin.String", false)))
        val task = KronosAtomicQueryTask("SELECT count", targetType = typeOf<MutableMap<String, Int>>(),
            resultColumns = mapOf("count" to stale))
        assertEquals(listOf(mapOf("COUNT" to 42)), KronosExecutionWrapper(source).toList(task))
        val bytes = byteArrayOf(0, 1, -1)
        val physical = RecordingSource(listOf("n" to KronosPhysicalValue.Null,
            "r" to KronosPhysicalValue.Real(2.5), "b" to KronosPhysicalValue.Blob(bytes)))
        val raw = KronosExecutionWrapper(physical).first(KronosAtomicQueryTask("SELECT n,r,b",
            targetType = typeOf<Map<String, *>>())) as Map<*, *>
        assertNull(raw["n"])
        assertEquals(2.5, raw["r"])
        assertContentEquals(bytes, raw["b"] as ByteArray)
    }

    @Test
    fun conflictingCaseFoldedMetadataIsRejectedBeforeExecution() {
        val source = RecordingSource(listOf("Count" to KronosPhysicalValue.Text("42")))
        val failure = assertFailsWith<ConflictingResultColumnLabels> {
            val task = KronosAtomicQueryTask("SELECT count", targetType = typeOf<Map<String, Any?>>(), resultColumns = mapOf(
                "COUNT" to ResultColumnMetadata(typeOf<Int>()), "count" to ResultColumnMetadata(typeOf<Boolean>())))
            KronosExecutionWrapper(source).first(task)
        }
        assertEquals(listOf("COUNT", "count"), failure.labels)
        assertEquals("Result column labels must be unique ignoring case: COUNT, count", failure.message)
        assertNull(source.query)
    }

    @Test
    fun explicitMapperSupportsLabelsAndExposesRowNumbersOnlyWhileLive() {
        val source = RecordingSource(listOf("ANSWER" to KronosPhysicalValue.Text("42")), rowCount = 2)
        val task = KronosAtomicQueryTask("SELECT answer", targetType = typeOf<Any?>(),
            resultColumns = mapOf("answer" to ResultColumnMetadata(typeOf<String>())))
        var escaped: KronosRow? = null
        val values = KronosExecutionWrapper(source).toList(task) {
            escaped = it
            it.rowNumber to it.get<Int>("ANSWER")
        }
        assertEquals(listOf(0 to 42, 1 to 42), values)
        assertFailsWith<IllegalStateException> { escaped!!.rowNumber }
        assertFailsWith<IllegalStateException> { escaped!!.get<Int>("ANSWER") }
        val noMetadata = task.copy(resultColumns = emptyMap())
        assertEquals(listOf(42, 42), KronosExecutionWrapper(source).toList(noMetadata) { it.get<Int>(1) })
    }

    @Test
    fun scalarAliasesUseSingleDeclaredMetadataButNeverStaleTargetTypes() {
        val source = RecordingSource(listOf("driver_label" to KronosPhysicalValue.Text("42")))
        val task = KronosAtomicQueryTask("SELECT answer", targetType = typeOf<Int>(),
            resultColumns = mapOf("answer" to ResultColumnMetadata(typeOf<String>(), typeInfo =
                com.kotlinorm.utils.StaticTypeInfo(com.kotlinorm.utils.StaticTypeRef("kotlin.String", false)))))
        assertEquals(42, KronosExecutionWrapper(source).first(task))
        assertEquals(42, KronosExecutionWrapper(source).first(task.copy(targetType = typeOf<Any?>(),
            resultColumns = mapOf("driver_label" to ResultColumnMetadata(typeOf<Int>())))))
        val noColumns = RecordingSource(allowZeroColumnRow = true)
        assertFailsWith<IllegalArgumentException> { KronosExecutionWrapper(noColumns).first(task) }
    }

    @Test
    fun pojoFallbackMatchesPhysicalNamesIgnoringCaseAndPreservesUnknownLabels() {
        val factory = Kronos.registerKPojoFactory(typeOf<AliasedRecord>()) { AliasedRecord() }
        try {
            val source = RecordingSource(listOf("FULL_NAME" to KronosPhysicalValue.Text("Kronos"),
                "unknown" to KronosPhysicalValue.Integer(1)))
            val record = KronosExecutionWrapper(source).first(KronosAtomicQueryTask("SELECT full_name,unknown",
                targetType = typeOf<AliasedRecord>())) as AliasedRecord
            assertEquals("Kronos", record.name)
            assertNull(record.name_1)
        } finally { factory.close() }
    }

    @Test
    fun emptyAndMismatchedBatchesNeverReachTheAdapter() {
        val source = RecordingSource()
        val wrapper = KronosExecutionWrapper(source)
        assertContentEquals(intArrayOf(), wrapper.batchUpdate(KronosAtomicBatchTask("DELETE FROM record")))
        assertNull(source.batch)
        val mismatched = KronosAtomicBatchTask("DELETE FROM record WHERE id IN (:ids)",
            arrayOf(mapOf("ids" to listOf(1)), mapOf("ids" to listOf(1, 2))), listParameterOccurrences = setOf(0))
        assertFailsWith<IllegalArgumentException> { wrapper.batchUpdate(mismatched) }
        assertNull(source.batch)
    }

    @Test
    fun batchCardinalityAndGeneratedKeysAreCheckedAndReplacePreviousState() {
        val task = KronosAtomicBatchTask("INSERT INTO record VALUES (:id)",
            arrayOf(mapOf("id" to 1), mapOf("id" to 2)), generatedKeyField = Field("id"),
            generatedKeys = mutableListOf("old"), lastInsertId = 99L)
        val incomplete = RecordingSource(batchResults = listOf(KronosExecutionResult(1)))
        assertFailsWith<IllegalStateException> { KronosExecutionWrapper(incomplete).batchUpdate(task) }
        assertEquals(listOf<Any?>("old"), task.generatedKeys)
        val complete = RecordingSource(batchResults = listOf(KronosExecutionResult(0),
            KronosExecutionResult(1, listOf(21L, 22L), 21L)))
        assertContentEquals(intArrayOf(0, 1), KronosExecutionWrapper(complete).batchUpdate(task))
        assertEquals(listOf<Any?>(21L, 22L), task.generatedKeys)
        assertEquals(21L, task.lastInsertId)
        val emptyKeys = RecordingSource(batchResults = listOf(KronosExecutionResult(0), KronosExecutionResult(0)))
        assertContentEquals(intArrayOf(0, 0), KronosExecutionWrapper(emptyKeys).batchUpdate(task))
        assertEquals(emptyList(), task.generatedKeys)
        assertNull(task.lastInsertId)
    }

    @Test
    fun mapDeclarationMatrixPreservesTypedRawNullableAndMutableBounds() {
        val wrapper = KronosExecutionWrapper(RecordingSource(listOf("id" to KronosPhysicalValue.Integer(7))))
        val typed = listOf(typeOf<Map<String, Int>>(), typeOf<Map<String?, Int?>>(),
            typeOf<Map<*, Int>>(), typeOf<MutableMap<String, Int>>(), typeOf<Map<String, Int>?>())
        typed.forEach { target ->
            assertEquals(listOf(mapOf("id" to 7)), wrapper.toList(KronosAtomicQueryTask("SELECT id", targetType = target)))
        }
        listOf(typeOf<Map<String, Any>>(), typeOf<Map<String, Any?>>(), typeOf<Map<String, *>>(),
            typeOf<Map<*, *>>(), typeOf<MutableMap<String?, Any?>>()).forEach { target ->
            assertEquals(listOf(mapOf("id" to 7L)), wrapper.toList(KronosAtomicQueryTask("SELECT id", targetType = target)))
        }
        val nullable = KronosExecutionWrapper(RecordingSource(listOf("id" to KronosPhysicalValue.Null)))
        listOf(typeOf<Map<String, Int?>>(), typeOf<Map<String, Any?>>(), typeOf<Map<*, *>>()).forEach { target ->
            assertEquals(listOf(mapOf("id" to null)), nullable.toList(KronosAtomicQueryTask("SELECT id", targetType = target)))
        }
        assertFailsWith<IllegalArgumentException> {
            nullable.toList(KronosAtomicQueryTask("SELECT id", targetType = typeOf<Map<String, Any>>()))
        }
    }

    @Test
    fun inferredAndTypedMapNumberTargetsPreserveAssignablePhysicalNumbersWithoutGuessingText() {
        val wrapper = KronosExecutionWrapper(RecordingSource(listOf("total" to KronosPhysicalValue.Integer(7))))
        val info = com.kotlinorm.utils.StaticTypeInfo(com.kotlinorm.utils.StaticTypeRef("kotlin.Number", true))
        val column = ResultColumnMetadata(typeOf<Number?>(), typeInfo = info)
        assertEquals(listOf(7L), wrapper.toList(KronosAtomicQueryTask("SELECT total", targetType = typeOf<Number?>(),
            targetTypeInfo = info, resultColumns = mapOf("total" to column))))
        assertEquals(listOf(7L), wrapper.toList(KronosAtomicQueryTask("SELECT total", targetType = typeOf<Number>(),
            resultColumns = mapOf("total" to column))))
        for (type in listOf(typeOf<Map<String, Number?>>(), typeOf<Map<String, Number>>())) {
            assertEquals(listOf(mapOf("total" to 7L)), wrapper.toList(KronosAtomicQueryTask("SELECT total", targetType = type,
                resultColumns = mapOf("total" to column))))
        }
        val nullable = KronosExecutionWrapper(RecordingSource(listOf("total" to KronosPhysicalValue.Null)))
        assertEquals(listOf(mapOf("total" to null)), nullable.toList(KronosAtomicQueryTask("SELECT total",
            targetType = typeOf<Map<String, Number?>>())))
        assertFailsWith<IllegalArgumentException> {
            nullable.toList(KronosAtomicQueryTask("SELECT total", targetType = typeOf<Number>()))
        }
        val text = KronosExecutionWrapper(RecordingSource(listOf("total" to KronosPhysicalValue.Text("7"))))
        assertFailsWith<com.kotlinorm.exceptions.ValueMappingException> {
            text.toList(KronosAtomicQueryTask("SELECT total", targetType = typeOf<Number?>()))
        }
    }

    @Test
    fun pojoDestinationsReplaceBroadProjectionMetadataForEachConcreteField() {
        val factory = Kronos.registerKPojoFactory(typeOf<NumericProjectionRecord>()) { NumericProjectionRecord() }
        try {
            val number = com.kotlinorm.utils.StaticTypeInfo(com.kotlinorm.utils.StaticTypeRef("kotlin.Number", true))
            val sourceField = Field("source_number", kType = typeOf<Number?>(), typeInfo = number)
            val metadata = ResultColumnMetadata(typeOf<Number?>(), sourceField)
            val task = KronosAtomicQueryTask("SELECT COUNT(*) AS total,SUM(score) AS scoreSum",
                targetType = typeOf<NumericProjectionRecord>(), resultColumns = mapOf("total" to metadata, "scoreSum" to metadata))
            val source = RecordingSource(listOf("total" to KronosPhysicalValue.Integer(4), "scoreSum" to KronosPhysicalValue.Integer(65)))
            val record = KronosExecutionWrapper(source).first(task) as NumericProjectionRecord
            assertEquals(4, record.total)
            assertEquals(65L, record.scoreSum)
        } finally { factory.close() }
    }

    @Test
    fun invalidMapDeclarationsAreRejectedEvenForEmptyQueries() {
        listOf(typeOf<Map<Int, Int>>(), typeOf<Map<Any, Int>>(),
            typeOf<Map<List<String>, Int>>(), typeOf<HashMap<String, Int>>()).forEach { target ->
            val source = RecordingSource()
            assertFailsWith<IllegalArgumentException> {
                KronosExecutionWrapper(source).toList(KronosAtomicQueryTask("SELECT id", targetType = target))
            }
            assertNull(source.query)
        }
        val source = RecordingSource()
        assertFailsWith<IllegalArgumentException> {
            KronosExecutionWrapper(source).toList(KronosAtomicQueryTask("SELECT id", targetType = typeOf<CustomMap>(),
                targetTypeInfo = com.kotlinorm.utils.StaticTypeInfo(typeOf<CustomMap>().toStaticTypeRef(),
                    containerKind = com.kotlinorm.utils.StaticContainerKind.MAP)))
        }
        assertNull(source.query)
    }

    @Test
    fun explicitRowsShareMetadataAndDecodeOnlyOncePerPositionAndTarget() {
        var calls = 0
        val codec = Kronos.registerValueCodec(valueCodec(
            supports = { _, context -> context.targetType == typeOf<Int>() },
            convert = { _, _ -> calls++; 7 }))
        try {
            val task = KronosAtomicQueryTask("SELECT id", targetType = typeOf<Any?>(),
                resultColumns = mapOf("id" to ResultColumnMetadata(typeOf<Int>())))
            val wrapper = KronosExecutionWrapper(RecordingSource(listOf("id" to KronosPhysicalValue.Text("7"))))
            assertEquals(KronosRowFirstResult.Present(listOf(7, 7)), wrapper.first(task) { row ->
                listOf(row.get<Any?>(1), row.get<Any?>("id"))
            })
            assertEquals(1, calls)
            val nulls = KronosExecutionWrapper(RecordingSource(listOf("id" to KronosPhysicalValue.Null)))
            assertFailsWith<IllegalArgumentException> {
                nulls.first(task.copy(resultColumns = emptyMap())) { it.get<Any>(1) }
            }
        } finally { codec.close() }
    }

    private interface CustomMap : Map<String, Int>

    @Suppress("ConstructorParameterNaming") // SQL projection aliases may contain numeric suffixes.
    internal class AliasedRecord(var name: String? = null, var name_1: String? = null) : KPojo {
        override var __kType: kotlin.reflect.KType
            get() = typeOf<AliasedRecord>()
            set(_) {}
        override var __tableName: String
            get() = "record"
            set(_) {}
        override var __tableComment: String
            get() = ""
            set(_) {}
        override var __columns: MutableList<Field>
            get() = mutableListOf(Field("full_name", "name", kType = typeOf<String?>()), Field("full_name_suffix", "name_1", kType = typeOf<String?>()))
            set(_) {}
        @Suppress("UNCHECKED_CAST")
        override fun <T : KPojo> fromMapData(map: Map<String, Any?>): T { name = map["name"] as String?; name_1 = map["name_1"] as String?; return this as T }
    }

    internal class NumericProjectionRecord(var total: Int? = null, var scoreSum: Long? = null) : KPojo {
        override var __kType: kotlin.reflect.KType = typeOf<NumericProjectionRecord>()
        override var __columns: MutableList<Field> = mutableListOf(Field("total", kType = typeOf<Int?>()),
            Field("scoreSum", kType = typeOf<Long?>()))
        @Suppress("UNCHECKED_CAST")
        override fun <T : KPojo> fromMapData(map: Map<String, Any?>): T {
            total = map["total"] as Int?
            scoreSum = map["scoreSum"] as Long?
            return this as T
        }
    }

    private class RecordingSource(private val columns: List<Pair<String, KronosPhysicalValue>> = emptyList(), private val rowCount: Int = 1, private val actionResult: KronosExecutionResult? = null,
        override val identity: KronosDatabaseIdentity = KronosDatabaseIdentity(DBType.SQLite, ":memory:"),
        private val batchResults: List<KronosExecutionResult>? = null,
        private val allowZeroColumnRow: Boolean = false) : KronosExecutionDataSource {
        var query: KronosQueryExecutionPlan? = null
        var action: KronosActionExecutionPlan? = null
        var batch: KronosBatchExecutionPlan? = null
        val transactions = mutableListOf<KronosTransactionOptions>()
        val savepoints = mutableListOf<String>()
        override fun <T> toList(plan: KronosQueryExecutionPlan, mapper: KronosPhysicalRowMapper<T>): List<T> {
            query = plan
            if (columns.isEmpty() && !allowZeroColumnRow) return emptyList()
            return List(rowCount) { index ->
            var active = true
            val row = object : KronosPhysicalRow {
                override val rowNumber = index
                override val columnCount get() = columns.size
                override val isActive get() = active
                override fun columnLabel(position: Int): String { requireColumnPosition(position); return columns[position - 1].first }
                override fun get(position: Int): KronosPhysicalValue { requireColumnPosition(position); return columns[position - 1].second }
            }
            try { mapper(row) } finally { active = false }
            }
        }
        override fun <T> first(plan: KronosQueryExecutionPlan, mapper: KronosPhysicalRowMapper<T>): KronosExecutionFirstResult<T> {
            val rows = toList(plan, mapper)
            return if (rows.isEmpty()) KronosExecutionFirstResult.Empty else KronosExecutionFirstResult.Present(rows.single())
        }
        override fun execute(plan: KronosActionExecutionPlan): KronosExecutionResult {
            action = plan
            return actionResult ?: KronosExecutionResult(1, if (plan.generatedKey != null) listOf(8L) else emptyList(), if (plan.generatedKey != null) 8L else null)
        }
        override fun executeBatch(plan: KronosBatchExecutionPlan): List<KronosExecutionResult> {
            batch = plan
            return batchResults ?: plan.entries.map { KronosExecutionResult(1) }
        }
        override fun <T> transact(options: KronosTransactionOptions, block: KronosTransactionScope.() -> T): T {
            transactions += options
            return object : KronosTransactionScope {
                override fun savepoint(name: String?): KronosSavepointToken {
                    savepoints += "savepoint:$name"
                    return object : KronosSavepointToken {}
                }
                override fun rollbackToSavepoint(savepoint: KronosSavepointToken) { savepoints += "rollback" }
                override fun releaseSavepoint(savepoint: KronosSavepointToken) { savepoints += "release" }
            }.block()
        }
    }
}
