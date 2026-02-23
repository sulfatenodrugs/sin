package com.example.sin

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "sizo.db"
        const val DATABASE_VERSION = 1

        // Таблица заключённых
        const val TABLE_PRISONERS = "prisoners"
        const val COLUMN_PRISONER_ID = "id"
        const val COLUMN_PRISONER_NAME = "full_name"
        const val COLUMN_BIRTH_DATE = "birth_date"
        const val COLUMN_PASSPORT = "passport"
        const val COLUMN_ADMISSION_DATE = "admission_date"
        const val COLUMN_RELEASE_DATE = "release_date"

        // Таблица личных вещей
        const val TABLE_BELONGINGS = "belongings"
        const val COLUMN_BELONGING_ID = "id"
        const val COLUMN_BELONGING_PRISONER_ID = "prisoner_id"
        const val COLUMN_BELONGING_DESC = "description"

        // Таблица посетителей
        const val TABLE_VISITORS = "visitors"
        const val COLUMN_VISITOR_ID = "id"
        const val COLUMN_VISITOR_NAME = "full_name"
        const val COLUMN_VISITOR_PASSPORT = "passport"

        // Таблица посещений
        const val TABLE_VISITS = "visits"
        const val COLUMN_VISIT_ID = "id"
        const val COLUMN_VISIT_VISITOR_ID = "visitor_id"
        const val COLUMN_VISIT_PRISONER_ID = "prisoner_id"
        const val COLUMN_VISIT_DATE = "visit_date"
        const val COLUMN_VISIT_TIME = "visit_time"
    }

    private val SQL_CREATE_PRISONERS = """
        CREATE TABLE $TABLE_PRISONERS (
            $COLUMN_PRISONER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_PRISONER_NAME TEXT NOT NULL,
            $COLUMN_BIRTH_DATE TEXT,
            $COLUMN_PASSPORT TEXT,
            $COLUMN_ADMISSION_DATE TEXT,
            $COLUMN_RELEASE_DATE TEXT
        )
    """.trimIndent()

    private val SQL_CREATE_BELONGINGS = """
        CREATE TABLE $TABLE_BELONGINGS (
            $COLUMN_BELONGING_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_BELONGING_PRISONER_ID INTEGER NOT NULL,
            $COLUMN_BELONGING_DESC TEXT NOT NULL,
            FOREIGN KEY($COLUMN_BELONGING_PRISONER_ID) REFERENCES $TABLE_PRISONERS($COLUMN_PRISONER_ID) ON DELETE CASCADE
        )
    """.trimIndent()

    private val SQL_CREATE_VISITORS = """
        CREATE TABLE $TABLE_VISITORS (
            $COLUMN_VISITOR_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_VISITOR_NAME TEXT NOT NULL,
            $COLUMN_VISITOR_PASSPORT TEXT
        )
    """.trimIndent()

    private val SQL_CREATE_VISITS = """
        CREATE TABLE $TABLE_VISITS (
            $COLUMN_VISIT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_VISIT_VISITOR_ID INTEGER NOT NULL,
            $COLUMN_VISIT_PRISONER_ID INTEGER NOT NULL,
            $COLUMN_VISIT_DATE TEXT NOT NULL,
            $COLUMN_VISIT_TIME TEXT NOT NULL,
            FOREIGN KEY($COLUMN_VISIT_VISITOR_ID) REFERENCES $TABLE_VISITORS($COLUMN_VISITOR_ID) ON DELETE CASCADE,
            FOREIGN KEY($COLUMN_VISIT_PRISONER_ID) REFERENCES $TABLE_PRISONERS($COLUMN_PRISONER_ID) ON DELETE CASCADE
        )
    """.trimIndent()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_PRISONERS)
        db.execSQL(SQL_CREATE_BELONGINGS)
        db.execSQL(SQL_CREATE_VISITORS)
        db.execSQL(SQL_CREATE_VISITS)

        // Добавляем тестовые данные (выполнится только при первом создании БД)
        insertTestData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VISITS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BELONGINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VISITORS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRISONERS")
        onCreate(db)
    }

    private fun insertTestData(db: SQLiteDatabase) {
        // Вставляем одного заключённого
        val prisonerValues = ContentValues().apply {
            put(COLUMN_PRISONER_NAME, "Иванов Иван Иванович")
            put(COLUMN_BIRTH_DATE, "1985-03-15")
            put(COLUMN_PASSPORT, "4510 123456")
            put(COLUMN_ADMISSION_DATE, "2023-01-10")
            put(COLUMN_RELEASE_DATE, "2025-01-09")
        }
        val prisonerId = db.insert(TABLE_PRISONERS, null, prisonerValues)

        // Вставляем одного посетителя
        val visitorValues = ContentValues().apply {
            put(COLUMN_VISITOR_NAME, "Петров Пётр Петрович")
            put(COLUMN_VISITOR_PASSPORT, "4520 654321")
        }
        val visitorId = db.insert(TABLE_VISITORS, null, visitorValues)

        // Вставляем тестовое посещение (если оба ID > 0)
        if (prisonerId > 0 && visitorId > 0) {
            val visitValues = ContentValues().apply {
                put(COLUMN_VISIT_VISITOR_ID, visitorId)
                put(COLUMN_VISIT_PRISONER_ID, prisonerId)
                put(COLUMN_VISIT_DATE, "2024-03-15")
                put(COLUMN_VISIT_TIME, "14:30")
            }
            db.insert(TABLE_VISITS, null, visitValues)
        }

        // Вставляем вещь для заключённого
        if (prisonerId > 0) {
            val belongingValues = ContentValues().apply {
                put(COLUMN_BELONGING_PRISONER_ID, prisonerId)
                put(COLUMN_BELONGING_DESC, "Куртка кожаная чёрная, размер 52")
            }
            db.insert(TABLE_BELONGINGS, null, belongingValues)
        }
    }
}