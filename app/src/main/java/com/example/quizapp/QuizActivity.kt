package com.example.quizapp


import android.content.Context
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import android.widget.ScrollView

class QuizActivity : AppCompatActivity() {
    private lateinit var scrollView: ScrollView

    private lateinit var mainLayout: LinearLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvQuestionNumber: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var tvResult: TextView
    private lateinit var tvScore: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnOptionA: Button
    private lateinit var btnOptionB: Button
    private lateinit var btnOptionC: Button
    private lateinit var btnOptionD: Button
    private lateinit var btnNext: Button
    private lateinit var btnClose: TextView
    private lateinit var questionCard: LinearLayout

    private lateinit var resultScreen: ScrollView
    private lateinit var tvFinalEmoji: TextView
    private lateinit var tvFinalTitle: TextView
    private lateinit var tvFinalScore: TextView
    private lateinit var tvFinalPercent: TextView
    private lateinit var btnRestart: Button
    private lateinit var btnBackToMenu: Button

    private var allQuestions = mutableListOf<Question>()
    private var questions = mutableListOf<Question>()
    private var currentIndex = 0
    private var answered = false
    private var correctCount = 0

    private var mode = "exam"
    private var ticketNumber = 1
    private val QUESTIONS_PER_TICKET = 60

    private var isDarkTheme = true

    private var correctSound: MediaPlayer? = null
    private var wrongSound: MediaPlayer? = null

    companion object {
        private const val TAG = "QuizActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        val prefs = getSharedPreferences("quiz_settings", Context.MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean("dark_theme", true)

        initViews()
        initSounds()
        applyTheme()

        mode = intent.getStringExtra("mode") ?: "exam"
        ticketNumber = intent.getIntExtra("ticketNumber", 1)

        loadAllQuestions()

        // Проверяем, загрузились ли вопросы
        Log.d(TAG, "Загружено вопросов: ${allQuestions.size}")

        if (allQuestions.isEmpty()) {
            Toast.makeText(this, "Ошибка: вопросы не загружены", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        prepareQuestions()

        // Проверяем, есть ли вопросы для этого билета
        if (questions.isEmpty()) {
            Toast.makeText(this, "В этом билете нет вопросов", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupClickListeners()
        showQuestion()
    }

    private fun initViews() {
        mainLayout = findViewById(R.id.mainLayout)
        tvTitle = findViewById(R.id.tvTitle)
        tvQuestionNumber = findViewById(R.id.tvQuestionNumber)
        tvQuestion = findViewById(R.id.tvQuestion)
        tvResult = findViewById(R.id.tvResult)
        tvScore = findViewById(R.id.tvScore)
        progressBar = findViewById(R.id.progressBar)
        btnOptionA = findViewById(R.id.btnOptionA)
        btnOptionB = findViewById(R.id.btnOptionB)
        btnOptionC = findViewById(R.id.btnOptionC)
        btnOptionD = findViewById(R.id.btnOptionD)
        btnNext = findViewById(R.id.btnNext)
        btnClose = findViewById(R.id.btnClose)
        questionCard = findViewById(R.id.questionCard)

        resultScreen = findViewById(R.id.resultScreen)
        tvFinalEmoji = findViewById(R.id.tvFinalEmoji)
        tvFinalTitle = findViewById(R.id.tvFinalTitle)
        tvFinalScore = findViewById(R.id.tvFinalScore)
        tvFinalPercent = findViewById(R.id.tvFinalPercent)
        btnRestart = findViewById(R.id.btnRestart)
        btnBackToMenu = findViewById(R.id.btnBackToMenu)
        scrollView = findViewById(R.id.scrollView)
    }

    private fun applyTheme() {
        if (isDarkTheme) {
            mainLayout.setBackgroundColor(resources.getColor(R.color.dark_background, null))
            questionCard.setBackgroundResource(R.drawable.card_background)
            tvTitle.setTextColor(resources.getColor(R.color.dark_text, null))
            tvQuestionNumber.setTextColor(resources.getColor(R.color.dark_text_secondary, null))
            tvQuestion.setTextColor(resources.getColor(R.color.dark_text, null))
            btnClose.setTextColor(resources.getColor(R.color.dark_text_secondary, null))
            tvFinalTitle.setTextColor(resources.getColor(R.color.dark_text, null))
            tvFinalScore.setTextColor(resources.getColor(R.color.dark_text_secondary, null))
        } else {
            mainLayout.setBackgroundColor(resources.getColor(R.color.light_background, null))
            questionCard.setBackgroundResource(R.drawable.card_background_light)
            tvTitle.setTextColor(resources.getColor(R.color.light_text, null))
            tvQuestionNumber.setTextColor(resources.getColor(R.color.light_text_secondary, null))
            tvQuestion.setTextColor(resources.getColor(R.color.light_text, null))
            btnClose.setTextColor(resources.getColor(R.color.light_text_secondary, null))
            tvFinalTitle.setTextColor(resources.getColor(R.color.light_text, null))
            tvFinalScore.setTextColor(resources.getColor(R.color.light_text_secondary, null))
        }
    }

    private fun getDefaultButtonColor(): ColorStateList {
        return if (isDarkTheme) {
            ColorStateList.valueOf(resources.getColor(R.color.dark_card, null))
        } else {
            ColorStateList.valueOf(resources.getColor(R.color.light_card, null))
        }
    }

    private fun initSounds() {
        try {
            correctSound = MediaPlayer.create(this, R.raw.correct)
            wrongSound = MediaPlayer.create(this, R.raw.wrong)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки звуков: ${e.message}")
        }
    }

    private fun playCorrectSound() {
        try {
            correctSound?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка воспроизведения звука: ${e.message}")
        }
    }

    private fun playWrongSound() {
        try {
            wrongSound?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка воспроизведения звука: ${e.message}")
        }

        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка вибрации: ${e.message}")
        }
    }

    private fun loadAllQuestions() {
        allQuestions.clear()

        try {
            val inputStream = assets.open("questions.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, Charsets.UTF_8)

            Log.d(TAG, "JSON размер: ${jsonString.length} символов")

            val jsonArray = JSONArray(jsonString)
            Log.d(TAG, "Найдено элементов в JSON: ${jsonArray.length()}")

            for (i in 0 until jsonArray.length()) {
                try {
                    val obj = jsonArray.getJSONObject(i)

                    // Проверяем наличие всех полей
                    if (!obj.has("question") || !obj.has("options") || !obj.has("correct")) {
                        Log.w(TAG, "Вопрос $i пропущен: отсутствуют обязательные поля")
                        continue
                    }

                    val questionText = obj.optString("question", "")
                    if (questionText.isEmpty()) {
                        Log.w(TAG, "Вопрос $i пропущен: пустой текст вопроса")
                        continue
                    }

                    val optionsObj = obj.optJSONObject("options")
                    if (optionsObj == null) {
                        Log.w(TAG, "Вопрос $i пропущен: нет вариантов ответов")
                        continue
                    }

                    val options = mutableMapOf<String, String>()
                    options["A"] = optionsObj.optString("A", "")
                    options["B"] = optionsObj.optString("B", "")
                    options["C"] = optionsObj.optString("C", "")
                    options["D"] = optionsObj.optString("D", "")

                    // Проверяем, что есть хотя бы 2 варианта ответа
                    val nonEmptyOptions = options.values.count { it.isNotEmpty() }
                    if (nonEmptyOptions < 2) {
                        Log.w(TAG, "Вопрос $i пропущен: мало вариантов ответов")
                        continue
                    }

                    val correct = obj.optString("correct", "")
                    if (correct.isEmpty() || correct !in listOf("A", "B", "C", "D")) {
                        Log.w(TAG, "Вопрос $i пропущен: неверный правильный ответ '$correct'")
                        continue
                    }

                    val question = Question(
                        question = questionText,
                        options = options,
                        correct = correct
                    )
                    allQuestions.add(question)

                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга вопроса $i: ${e.message}")
                }
            }

            Log.d(TAG, "Успешно загружено вопросов: ${allQuestions.size}")

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки JSON: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun prepareQuestions() {
        questions.clear()
        currentIndex = 0
        correctCount = 0

        when (mode) {
            "ticket" -> {
                tvTitle.text = "Билет $ticketNumber"
                val startIndex = (ticketNumber - 1) * QUESTIONS_PER_TICKET
                val endIndex = minOf(startIndex + QUESTIONS_PER_TICKET, allQuestions.size)

                Log.d(TAG, "Билет $ticketNumber: индексы $startIndex - $endIndex")

                if (startIndex < allQuestions.size) {
                    for (i in startIndex until endIndex) {
                        questions.add(allQuestions[i])
                    }
                }

                Log.d(TAG, "Вопросов в билете: ${questions.size}")
            }
            "exam" -> {
                tvTitle.text = "Тестирование"
                val shuffled = allQuestions.shuffled()
                val count = minOf(60, shuffled.size)
                for (i in 0 until count) {
                    questions.add(shuffled[i])
                }
            }
        }

        updateScore()
    }

    private fun setupClickListeners() {
        btnOptionA.setOnClickListener { checkAnswer("A", btnOptionA) }
        btnOptionB.setOnClickListener { checkAnswer("B", btnOptionB) }
        btnOptionC.setOnClickListener { checkAnswer("C", btnOptionC) }
        btnOptionD.setOnClickListener { checkAnswer("D", btnOptionD) }

        btnNext.setOnClickListener {
            currentIndex++
            if (currentIndex < questions.size) {
                showQuestion()
            } else {
                showResults()
            }
        }

        btnClose.setOnClickListener {
            finish()
        }

        btnRestart.setOnClickListener {
            prepareQuestions()
            resultScreen.visibility = View.GONE
            showQuestionViews()
            showQuestion()
        }

        btnBackToMenu.setOnClickListener {
            finish()
        }
    }

    private fun showQuestion() {
        if (currentIndex >= questions.size) {
            showResults()
            return
        }

        answered = false
        val q = questions[currentIndex]

        tvQuestionNumber.text = "Вопрос ${currentIndex + 1} из ${questions.size}"
        tvQuestion.text = q.question
        tvResult.text = ""

        val progress = ((currentIndex.toFloat() / questions.size.toFloat()) * 100).toInt()
        progressBar.progress = progress

        btnOptionA.text = "A:  ${q.options["A"] ?: ""}"
        btnOptionB.text = "B:  ${q.options["B"] ?: ""}"
        btnOptionC.text = "C:  ${q.options["C"] ?: ""}"
        btnOptionD.text = "D:  ${q.options["D"] ?: ""}"

        resetButtonStyles()
        setButtonsEnabled(true)
        btnNext.visibility = View.GONE

        // Прокрутка вверх для нового вопроса
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_UP)
        }
    }

    private fun checkAnswer(selected: String, button: Button) {
        if (answered) return
        answered = true

        val q = questions[currentIndex]
        val correctAnswer = q.correct

        val greenColor = ColorStateList.valueOf(resources.getColor(R.color.correct_green, null))
        val redColor = ColorStateList.valueOf(resources.getColor(R.color.wrong_red, null))

        if (selected == correctAnswer) {
            correctCount++
            button.backgroundTintList = greenColor
            tvResult.text = "✓ Правильно!"
            tvResult.setTextColor(resources.getColor(R.color.correct_green, null))
            playCorrectSound()
        } else {
            button.backgroundTintList = redColor
            tvResult.text = "✗ Неправильно! Правильный ответ: $correctAnswer"
            tvResult.setTextColor(resources.getColor(R.color.wrong_red, null))
            highlightCorrectAnswer(correctAnswer)
            playWrongSound()
        }

        updateScore()
        setButtonsEnabled(false)
        btnNext.visibility = View.VISIBLE

        // Автопрокрутка вниз чтобы показать кнопку "Далее"
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun updateScore() {
        tvScore.text = "$correctCount/${currentIndex + if (answered) 1 else 0}"
    }

    private fun highlightCorrectAnswer(correct: String) {
        val greenColor = ColorStateList.valueOf(resources.getColor(R.color.correct_green, null))
        when (correct) {
            "A" -> btnOptionA.backgroundTintList = greenColor
            "B" -> btnOptionB.backgroundTintList = greenColor
            "C" -> btnOptionC.backgroundTintList = greenColor
            "D" -> btnOptionD.backgroundTintList = greenColor
        }
    }

    private fun resetButtonStyles() {
        val defaultColor = getDefaultButtonColor()
        btnOptionA.backgroundTintList = defaultColor
        btnOptionB.backgroundTintList = defaultColor
        btnOptionC.backgroundTintList = defaultColor
        btnOptionD.backgroundTintList = defaultColor

        val textColor = if (isDarkTheme) {
            resources.getColor(R.color.dark_text, null)
        } else {
            resources.getColor(R.color.light_text, null)
        }
        btnOptionA.setTextColor(textColor)
        btnOptionB.setTextColor(textColor)
        btnOptionC.setTextColor(textColor)
        btnOptionD.setTextColor(textColor)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnOptionA.isEnabled = enabled
        btnOptionB.isEnabled = enabled
        btnOptionC.isEnabled = enabled
        btnOptionD.isEnabled = enabled
    }

    private fun hideQuestionViews() {
        tvQuestion.visibility = View.GONE
        tvQuestionNumber.visibility = View.GONE
        tvResult.visibility = View.GONE
        tvScore.visibility = View.GONE
        progressBar.visibility = View.GONE
        questionCard.visibility = View.GONE
        btnOptionA.visibility = View.GONE
        btnOptionB.visibility = View.GONE
        btnOptionC.visibility = View.GONE
        btnOptionD.visibility = View.GONE
        btnNext.visibility = View.GONE
        scrollView.visibility = View.GONE
    }

    private fun showQuestionViews() {
        tvQuestion.visibility = View.VISIBLE
        tvQuestionNumber.visibility = View.VISIBLE
        tvResult.visibility = View.VISIBLE
        tvScore.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        questionCard.visibility = View.VISIBLE
        btnOptionA.visibility = View.VISIBLE
        btnOptionB.visibility = View.VISIBLE
        btnOptionC.visibility = View.VISIBLE
        btnOptionD.visibility = View.VISIBLE
        scrollView.visibility = View.VISIBLE
    }

    private fun showResults() {
        hideQuestionViews()
        resultScreen.visibility = View.VISIBLE

        val total = questions.size
        val percent = if (total > 0) (correctCount * 100) / total else 0

        tvFinalScore.text = "Правильных: $correctCount из $total"
        tvFinalPercent.text = "$percent%"

        when {
            percent >= 90 -> {
                tvFinalEmoji.text = "🎉"
                tvFinalTitle.text = "Отлично!"
                tvFinalPercent.setTextColor(resources.getColor(R.color.correct_green, null))
            }
            percent >= 70 -> {
                tvFinalEmoji.text = "👍"
                tvFinalTitle.text = "Хорошо!"
                tvFinalPercent.setTextColor(resources.getColor(R.color.warning_yellow, null))
            }
            else -> {
                tvFinalEmoji.text = "📚"
                tvFinalTitle.text = "Нужно повторить"
                tvFinalPercent.setTextColor(resources.getColor(R.color.wrong_red, null))
            }
        }

        if (mode == "ticket") {
            val prefs = getSharedPreferences("quiz_progress", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putInt("ticket_${ticketNumber}_score", correctCount)
            editor.putInt("ticket_${ticketNumber}_total", total)
            editor.apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        correctSound?.release()
        wrongSound?.release()

    }
}