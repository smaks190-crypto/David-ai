package com.example.data.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val isIncome: Boolean,
    val timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"))
)
