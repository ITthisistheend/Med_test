package com.example.quizapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class TicketListActivity : AppCompatActivity() {

    private val QUESTIONS_PER_TICKET = 60
    private lateinit var ticketsContainer: LinearLayout
    private lateinit var mainLayout: LinearLayout
    private var isDarkTheme = true

    companion object {
        private const val TAG = "TicketListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_list)

        val prefs = getSharedPreferences("quiz_settings", Context.MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean("dark_theme", true)

        mainLayout = findViewById(R.id.mainLayout)
        ticketsContainer = findViewById(R.id.ticketsContainer)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        applyTheme()
        createTicketButtons()
    }

    override fun onResume() {
        super.onResume()
        updateTicketColors()
    }

    private fun applyTheme() {
        if (isDarkTheme) {
            mainLayout.setBackgroundColor(resources.getColor(R.color.dark_background, null))
        } else {
            mainLayout.setBackgroundColor(resources.getColor(R.color.light_background, null))
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
                    // пропускаем
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка: ${e.message}")
        }
        return count
    }

    private fun createTicketButtons() {
        ticketsContainer.removeAllViews()

        val totalQuestions = countValidQuestions()
        val ticketsCount = if (totalQuestions > 0) (totalQuestions + QUESTIONS_PER_TICKET - 1) / QUESTIONS_PER_TICKET else 0

        Log.d(TAG, "Всего вопросов: $totalQuestions, билетов: $ticketsCount")

        for (i in 1..ticketsCount) {
            val button = Button(this)
            button.id = i

            // Подсчитаем сколько вопросов в этом билете
            val startIndex = (i - 1) * QUESTIONS_PER_TICKET
            val endIndex = minOf(startIndex + QUESTIONS_PER_TICKET, totalQuestions)
            val questionsInTicket = endIndex - startIndex

            button.text = "Билет $i ($questionsInTicket вопр.)"
            button.textSize = 16f
            button.setTextColor(Color.WHITE)

            if (isDarkTheme) {
                button.setBackgroundResource(R.drawable.ticket_button_default)
            } else {
                button.setBackgroundResource(R.drawable.ticket_button_light)
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                140
            )
            params.bottomMargin = 12
            button.layoutParams = params

            button.setOnClickListener {
                val intent = Intent(this, QuizActivity::class.java)
                intent.putExtra("mode", "ticket")
                intent.putExtra("ticketNumber", i)
                startActivity(intent)
            }

            ticketsContainer.addView(button)
        }

        updateTicketColors()
    }

    private fun updateTicketColors() {
        val prefs = getSharedPreferences("quiz_progress", Context.MODE_PRIVATE)
        val totalQuestions = countValidQuestions()
        val ticketsCount = if (totalQuestions > 0) (totalQuestions + QUESTIONS_PER_TICKET - 1) / QUESTIONS_PER_TICKET else 0

        for (i in 1..ticketsCount) {
            val button = ticketsContainer.findViewById<Button>(i) ?: continue
            val score = prefs.getInt("ticket_${i}_score", -1)
            val total = prefs.getInt("ticket_${i}_total", -1)

            if (score >= 0 && total > 0) {
                val percent = (score * 100) / total
                when {
                    percent >= 90 -> {
                        button.setBackgroundResource(R.drawable.ticket_button_passed)
                        button.text = "Билет $i  ✓  $percent%"
                    }
                    percent >= 70 -> {
                        button.setBackgroundResource(R.drawable.ticket_button_medium)
                        button.text = "Билет $i  •  $percent%"
                        button.setTextColor(Color.BLACK)
                    }
                    else -> {
                        button.setBackgroundResource(R.drawable.ticket_button_failed)
                        button.text = "Билет $i  •  $percent%"
                    }
                }
            }
        }
    }
}