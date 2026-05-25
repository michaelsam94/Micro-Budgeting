package com.example.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String
)

@Entity(
    tableName = "transactions"
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val note: String,
    val timestamp: Long,
    val source: String, // "MANUAL" or "SMS"
    val rawSms: String? = null
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "month"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val limitAmount: Double,
    val month: String // "YYYY-MM"
)
