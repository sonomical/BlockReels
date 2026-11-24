package com.blockreels.app.state

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.blockreels.app.detection.DetectionLayer
import java.util.UUID

private fun blockDiagnosticCode(value: String): String = value.takeIf { code ->
    code == "REEL_CONTAINER_MATCH" || code == "SURGICAL_HEURISTIC" || code == "CLIPS_CONTAINER" ||
        DetectionLayer.entries.any { it.name == code }
} ?: "REEL_CONTAINER_MATCH"

internal data class PendingBlockEvent(
    val eventId: String,
    val timestamp: Long,
    val packageName: String,
    val appType: String,
    val interventionMode: String,
    val detectionReason: String
) {
    fun toWireMap(): Map<String, Any> = mapOf(
        "eventType" to "REEL_BLOCKED",
        "durablyQueued" to true,
        "eventId" to eventId,
        "timestamp" to timestamp,
        "packageName" to packageName,
        "appType" to appType,
        "interventionMode" to interventionMode,
        "detectionReason" to detectionReason
    )

    companion object {
        fun create(timestamp: Long, packageName: String, appType: String,
                   mode: String, diagnosticCode: String): PendingBlockEvent = PendingBlockEvent(
            UUID.randomUUID().toString(), timestamp, packageName, appType, mode,
            blockDiagnosticCode(diagnosticCode)
        )
    }
}

/** Private outbox. Reads never remove rows; only explicit acknowledgements do. */
internal class PendingBlockEventStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "pending_block_events.db", null, 1,
        DatabaseErrorHandler { throw SQLiteException("Pending block event storage is unavailable") }) {
    companion object {
        @Volatile private var instance: PendingBlockEventStore? = null
        fun getInstance(context: Context): PendingBlockEventStore =
            instance ?: synchronized(this) {
                instance ?: PendingBlockEventStore(context).also { instance = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE pending_block_events (
                event_id TEXT PRIMARY KEY NOT NULL,
                timestamp INTEGER NOT NULL,
                package_name TEXT NOT NULL,
                app_type TEXT NOT NULL,
                intervention_mode TEXT NOT NULL,
                detection_reason TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX pending_block_event_order ON pending_block_events(timestamp)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw SQLiteException("Unsupported pending block event schema")
    }

    @Volatile var generation: Long = 0
        private set

    @Synchronized
    fun clearPending() {
        writableDatabase.delete("pending_block_events", null, null)
        generation++
    }

    @Synchronized
    fun insert(event: PendingBlockEvent, expectedGeneration: Long): Boolean {
        if (expectedGeneration != generation) return false
        val values = ContentValues().apply {
            put("event_id", event.eventId)
            put("timestamp", event.timestamp)
            put("package_name", event.packageName)
            put("app_type", event.appType)
            put("intervention_mode", event.interventionMode)
            put("detection_reason", blockDiagnosticCode(event.detectionReason))
        }
        writableDatabase.insertOrThrow("pending_block_events", null, values)
        return true
    }

    fun readPending(limit: Int = 100): List<Map<String, Any>> {
        val events = mutableListOf<Map<String, Any>>()
        readableDatabase.query(
            "pending_block_events",
            arrayOf("event_id", "timestamp", "package_name", "app_type", "intervention_mode", "detection_reason"),
            null, null, null, null, "timestamp ASC, rowid ASC", limit.coerceIn(1, 100).toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                events.add(PendingBlockEvent(
                    cursor.getString(0), cursor.getLong(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getString(5)
                ).toWireMap())
            }
        }
        return events
    }

    fun acknowledge(ids: List<String>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            ids.distinct().chunked(100).forEach { batch ->
                db.delete("pending_block_events",
                    "event_id IN (${batch.joinToString(",") { "?" }})", batch.toTypedArray())
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
