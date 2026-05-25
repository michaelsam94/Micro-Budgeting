package com.example.domain.repository

import com.example.domain.model.Budget
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun addCategory(category: Category): Long
    suspend fun getCategoryById(id: Long): Category?
    suspend fun deleteCategory(id: Long)
    suspend fun insertDefaultCategoriesIfNeeded()
}

interface TransactionRepository {
    fun getTransactionsByMonth(month: String): Flow<List<Transaction>>
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun addTransaction(transaction: Transaction): Long
    suspend fun deleteTransaction(id: Long)
    suspend fun getTransactionsByCategory(categoryId: Long, month: String): List<Transaction>
}

interface BudgetRepository {
    fun getBudgetsByMonth(month: String): Flow<List<Budget>>
    suspend fun setBudget(budget: Budget): Long
    suspend fun deleteBudget(id: Long)
}
