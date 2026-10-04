package com.example.uniportal.testutil

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.uniportal.data.local.UniPortalDatabase
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun inMemoryDatabase(): UniPortalDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), UniPortalDatabase::class.java)
        .allowMainThreadQueries()
        .build()
        .also { database ->

            database.openHelper.writableDatabase
        }

fun fileDatabase(name: String): UniPortalDatabase =
    Room.databaseBuilder(ApplicationProvider.getApplicationContext<Context>(), UniPortalDatabase::class.java, name)
        .allowMainThreadQueries()

        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
        .build()

val ROOM_FLOW_TIMEOUT: Duration = 10.seconds

fun UniPortalDatabase.countRows(table: String): Int =
    openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

fun UniPortalDatabase.rawText(table: String, column: String, keyColumn: String, key: String): String =
    openHelper.readableDatabase.query("SELECT $column FROM $table WHERE $keyColumn = ?", arrayOf<Any?>(key))
        .use { cursor ->
            check(cursor.moveToFirst()) { "No row with $keyColumn = $key in $table" }
            cursor.getString(0)
        }

fun UniPortalDatabase.rawLong(table: String, column: String, keyColumn: String, key: String): Long =
    openHelper.readableDatabase.query("SELECT $column FROM $table WHERE $keyColumn = ?", arrayOf<Any?>(key))
        .use { cursor ->
            check(cursor.moveToFirst()) { "No row with $keyColumn = $key in $table" }
            cursor.getLong(0)
        }

fun UniPortalDatabase.rawColumn(table: String, column: String, orderBy: String): List<String> =
    openHelper.readableDatabase.query("SELECT $column FROM $table ORDER BY $orderBy").use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
    }
