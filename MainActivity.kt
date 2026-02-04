package com.example.quizapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var mainLayout: LinearLayout
    private lateinit var tvAppTitle: TextView
    private lateinit var tvTotalQuestions: TextView
    private lateinit var btnTheme: TextView

    private var isDarkTheme = true

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("quiz_settings", Context.MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean("dark_theme", true)
        applyTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainLayout = findViewById(R.id.mainLayout)
        tvAppTitle = findViewById(R.id.tvAppTitle)
        tvTotalQuestions = findViewById(R.id.tvTotalQuestions)
        btnTheme = findViewById(R.id.btnTheme)
        val btnTickets = findViewById<Button>(R.id.btnTickets)
        val btnExam = findViewById<Button>(R.id.btnExam)

        // Подсчёт РЕАЛЬНЫХ вопросов
        val totalQuestions = countValidQuestions()
        val ticketsCount = if (totalQuestions > 0) (totalQuestions + 59) / 60 else 0
        tvTotalQuestions.text = "Всего вопросов: $totalQuestions (билетов: $ticketsCount)"

        updateColors()

        btnTheme.setOnClickListener {
            isDarkTheme = !isDarkTheme
            val editor = prefs.edit()
            editor.putBoolean("dark_theme", isDarkTheme)
            editor.apply()
            applyTheme()
            recreate()
        }

        btnTickets.setOnClickListener {
            val intent = Intent(this, TicketListActivity::class.java)
            startActivity(intent)
        }

        btnExam.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("mode", "exam")
            startActivity(intent)
        }
    }

    private fun applyTheme() {
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun updateColors() {
        if (isDarkTheme) {
            btnTheme.text = "☀️"
            mainLayout.setBackgroundColor(resources.getColor(R.color.dark_background, null))
            tvAppTitle.setTextColor(resources.getColor(R.color.dark_text, null))
            tvTotalQuestions.setTextColor(resources.getColor(R.color.dark_text_secondary, null))
        } else {
            btnTheme.text = "🌙"
            mainLayout.setBackgroundColor(resources.getColor(R.color.light_background, null))
            tvAppTitle.setTextColor(resources.getColor(R.color.light_text, null))
            tvTotalQuestions.setTextColor(resources.getColor(R.color.light_text_secondary, null))
        }
    }

    private fun countValidQuestions(): Int {
        var count = 0
        try {
            val inputStream = assets.open("questions.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, Charsets.UTF_8)

            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                try {
                    val obj = jsonArray.getJSONObject(i)

                    // Проверяем что вопрос валидный
                    val question = obj.optString("question", "")
                    val options = obj.optJSONObject("options")
                    val correct = obj.optString("correct", "")

                    if (question.isNotEmpty() &&
                        options != null &&
                        correct in listOf("A", "B", "C", "D")) {

                        val a = options.optString("A", "")
                        val b = options.optString("B", "")

                        if (a.isNotEmpty() && b.isNotEmpty()) {
                            count++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка проверки вопроса $i: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка чтения JSON: ${e.message}")
        }

        Log.d(TAG, "Найдено валидных вопросов: $count")
        return count
    }
}