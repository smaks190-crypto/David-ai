package com.example.data.repository

import com.example.data.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BudgetRepository {

    private val _transactions = MutableStateFlow<List<Transaction>>(
        listOf(
            Transaction(
                title = "Аванс и премия",
                amount = 45000.0,
                category = "Доходы",
                isIncome = true,
                timestamp = "Вчера, 14:20"
            ),
            Transaction(
                title = "Супермаркет Перекресток",
                amount = 2850.0,
                category = "Продукты",
                isIncome = false,
                timestamp = "Вчера, 19:40"
            ),
            Transaction(
                title = "Яндекс Такси",
                amount = 480.0,
                category = "Транспорт",
                isIncome = false,
                timestamp = "Сегодня, 08:35"
            ),
            Transaction(
                title = "Кофе и круассан",
                amount = 320.0,
                category = "Кафе и Фастфуд",
                isIncome = false,
                timestamp = "Сегодня, 09:15"
            )
        )
    )
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    fun addTransaction(title: String, amount: Double, category: String, isIncome: Boolean) {
        val newTx = Transaction(
            title = title,
            amount = amount,
            category = category,
            isIncome = isIncome,
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("Сегодня, HH:mm"))
        )
        val current = _transactions.value.toMutableList()
        current.add(0, newTx)
        _transactions.value = current
    }

    fun addTransactions(newItems: List<Transaction>) {
        val current = _transactions.value.toMutableList()
        current.addAll(0, newItems)
        _transactions.value = current
    }

    fun deleteTransaction(id: String) {
        val current = _transactions.value.toMutableList()
        current.removeAll { it.id == id }
        _transactions.value = current
    }
}
