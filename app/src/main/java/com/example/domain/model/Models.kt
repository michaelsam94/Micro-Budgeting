package com.example.domain.model

enum class TransactionSource {
    MANUAL, SMS
}

data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String, // e.g. "shopping_cart", "restaurant", "directions_car", "local_hospital", "bolt", "local_mall"
    val colorHex: String // e.g. "#FF5722"
)

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val category: Category? = null,
    val note: String,
    val timestamp: Long,
    val source: TransactionSource,
    val rawSms: String? = null
)

data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val limitAmount: Double,
    val month: String // Formatted as "YYYY-MM"
)

data class BudgetSummary(
    val category: Category,
    val budget: Budget?, // Null means no budget set for this category
    val spent: Double,
    val remaining: Double,
    val percentUsed: Float
)
