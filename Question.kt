package com.example.quizapp

data class Question(
    val question: String,
    val options: Map<String, String>,
    val correct: String
)