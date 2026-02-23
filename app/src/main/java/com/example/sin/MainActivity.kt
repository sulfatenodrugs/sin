package com.example.sin

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PrisonerItem(val id: Long, val name: String)

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val dataList = mutableListOf<String>()
    private var prisonerItems: List<PrisonerItem> = emptyList()
    private var currentMode = "prisoners" // "prisoners" или "visits"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listViewPrisoners)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dataList)
        listView.adapter = adapter

        // Кнопка добавления заключённого
        val btnAddPrisoner = findViewById<Button>(R.id.btnAddPrisoner)
        btnAddPrisoner.setOnClickListener {
            addTestPrisoner()
        }

        // Кнопка добавления посещения
        val btnAddVisit = findViewById<Button>(R.id.btnAddVisit)
        btnAddVisit.setOnClickListener {
            addTestVisit()
        }

        // Кнопка показа посещений
        val btnShowVisits = findViewById<Button>(R.id.btnShowVisits)
        btnShowVisits.setOnClickListener {
            loadVisits()
        }

        // Обработка клика по элементу списка
        listView.setOnItemClickListener { _, _, position, _ ->
            when (currentMode) {
                "prisoners" -> {
                    if (position < prisonerItems.size) {
                        val prisonerId = prisonerItems[position].id
                        showPrisonerDetails(prisonerId)
                    }
                }
                "visits" -> {
                    Toast.makeText(this, "Это посещение. Детали пока не реализованы.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Загружаем заключённых по умолчанию
        loadPrisoners()
    }

    // ---------- Заключённые ----------
    private fun loadPrisoners() {
        currentMode = "prisoners"
        lifecycleScope.launch(Dispatchers.IO) {
            val prisoners = fetchPrisonersFromDb()
            withContext(Dispatchers.Main) {
                prisonerItems = prisoners
                dataList.clear()
                dataList.addAll(prisoners.map { it.name })
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun fetchPrisonersFromDb(): List<PrisonerItem> {
        val prisoners = mutableListOf<PrisonerItem>()
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_PRISONERS,
            arrayOf(DatabaseHelper.COLUMN_PRISONER_ID, DatabaseHelper.COLUMN_PRISONER_NAME),
            null, null, null, null, null
        )
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRISONER_ID)
            val nameCol = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRISONER_NAME)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val name = it.getString(nameCol)
                prisoners.add(PrisonerItem(id, name))
            }
        }
        db.close()
        return prisoners
    }

    private fun addTestPrisoner() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dbHelper = DatabaseHelper(this@MainActivity)
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_PRISONER_NAME, "Новый заключённый ${System.currentTimeMillis()}")
                put(DatabaseHelper.COLUMN_BIRTH_DATE, "2000-01-01")
                put(DatabaseHelper.COLUMN_PASSPORT, "1234 567890")
                put(DatabaseHelper.COLUMN_ADMISSION_DATE, "2024-01-01")
                put(DatabaseHelper.COLUMN_RELEASE_DATE, "2026-01-01")
            }
            db.insert(DatabaseHelper.TABLE_PRISONERS, null, values)
            db.close()
            withContext(Dispatchers.Main) {
                loadPrisoners()  // после добавления показываем список заключённых
            }
        }
    }

    private fun showPrisonerDetails(prisonerId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val details = fetchPrisonerDetails(prisonerId)
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Данные заключённого")
                    .setMessage(details)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun fetchPrisonerDetails(prisonerId: Long): String {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_PRISONERS,
            null,
            "${DatabaseHelper.COLUMN_PRISONER_ID} = ?",
            arrayOf(prisonerId.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val name = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRISONER_NAME))
                val birth = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BIRTH_DATE))
                val passport = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSPORT))
                val admission = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADMISSION_DATE))
                val release = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RELEASE_DATE))
                db.close()
                return """
                    ФИО: $name
                    Дата рождения: $birth
                    Паспорт: $passport
                    Дата поступления: $admission
                    Ориентировочная дата освобождения: $release
                """.trimIndent()
            }
        }
        db.close()
        return "Информация не найдена"
    }

    // ---------- Посещения ----------
    private fun loadVisits() {
        currentMode = "visits"
        lifecycleScope.launch(Dispatchers.IO) {
            val visits = fetchVisitsFromDb()
            withContext(Dispatchers.Main) {
                dataList.clear()
                dataList.addAll(visits)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun fetchVisitsFromDb(): List<String> {
        val visits = mutableListOf<String>()
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val query = """
            SELECT v.${DatabaseHelper.COLUMN_VISIT_DATE},
                   v.${DatabaseHelper.COLUMN_VISIT_TIME},
                   vis.${DatabaseHelper.COLUMN_VISITOR_NAME},
                   p.${DatabaseHelper.COLUMN_PRISONER_NAME}
            FROM ${DatabaseHelper.TABLE_VISITS} v
            JOIN ${DatabaseHelper.TABLE_VISITORS} vis
                ON v.${DatabaseHelper.COLUMN_VISIT_VISITOR_ID} = vis.${DatabaseHelper.COLUMN_VISITOR_ID}
            JOIN ${DatabaseHelper.TABLE_PRISONERS} p
                ON v.${DatabaseHelper.COLUMN_VISIT_PRISONER_ID} = p.${DatabaseHelper.COLUMN_PRISONER_ID}
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                val date = it.getString(0)
                val time = it.getString(1)
                val visitor = it.getString(2)
                val prisoner = it.getString(3)
                visits.add("$visitor -> $prisoner, $date $time")
            }
        }
        db.close()
        return visits
    }

    private fun addTestVisit() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dbHelper = DatabaseHelper(this@MainActivity)
            val db = dbHelper.writableDatabase

            // Проверим, есть ли посетитель с id=1 и заключённый с id=1
            // Для простоты вставим, если они есть, иначе создадим их
            ensureVisitorAndPrisonerExist(db)

            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_VISIT_VISITOR_ID, 1)
                put(DatabaseHelper.COLUMN_VISIT_PRISONER_ID, 1)
                put(DatabaseHelper.COLUMN_VISIT_DATE, "2024-03-15")
                put(DatabaseHelper.COLUMN_VISIT_TIME, "14:30")
            }
            db.insert(DatabaseHelper.TABLE_VISITS, null, values)
            db.close()
            withContext(Dispatchers.Main) {
                loadVisits() // после добавления показываем список посещений
            }
        }
    }

    private fun ensureVisitorAndPrisonerExist(db: SQLiteDatabase) {
        // Проверяем наличие заключённого с id=1, если нет - создаём
        var cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_PRISONERS} WHERE ${DatabaseHelper.COLUMN_PRISONER_ID} = 1", null)
        if (!cursor.moveToFirst()) {
            val prisonerValues = ContentValues().apply {
                put(DatabaseHelper.COLUMN_PRISONER_NAME, "Иванов Иван (авто)")
                put(DatabaseHelper.COLUMN_BIRTH_DATE, "1980-01-01")
                put(DatabaseHelper.COLUMN_PASSPORT, "1111 111111")
                put(DatabaseHelper.COLUMN_ADMISSION_DATE, "2020-01-01")
                put(DatabaseHelper.COLUMN_RELEASE_DATE, "2030-01-01")
            }
            db.insert(DatabaseHelper.TABLE_PRISONERS, null, prisonerValues)
        }
        cursor.close()

        // Проверяем наличие посетителя с id=1
        cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_VISITORS} WHERE ${DatabaseHelper.COLUMN_VISITOR_ID} = 1", null)
        if (!cursor.moveToFirst()) {
            val visitorValues = ContentValues().apply {
                put(DatabaseHelper.COLUMN_VISITOR_NAME, "Петров Пётр (авто)")
                put(DatabaseHelper.COLUMN_VISITOR_PASSPORT, "2222 222222")
            }
            db.insert(DatabaseHelper.TABLE_VISITORS, null, visitorValues)
        }
        cursor.close()
    }
}