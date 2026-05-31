package com.michael.microbudgeting.playstore

import android.content.Context
import com.michael.microbudgeting.domain.model.*
import com.michael.microbudgeting.domain.repository.*
import com.michael.microbudgeting.presentation.FinanceViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MockCategoryRepository : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> = flowOf(listOf(
        Category(1, "Groceries", "shopping_cart", "#FF5722"),
        Category(2, "Transport", "directions_car", "#2196F3"),
        Category(3, "Entertainment", "movie", "#9C27B0"),
        Category(4, "Rent", "home", "#4CAF50")
    ))
    override suspend fun addCategory(category: Category): Long = 0
    override suspend fun getCategoryById(id: Long): Category? = null
    override suspend fun deleteCategory(id: Long) {}
    override suspend fun insertDefaultCategoriesIfNeeded() {}
}

class MockTransactionRepository : TransactionRepository {
    override fun getTransactionsByMonth(month: String): Flow<List<Transaction>> = flowOf(listOf(
        Transaction(id = 1, amount = 450.0, categoryId = 1, note = "Weekly groceries purchase", timestamp = System.currentTimeMillis() - 86400000, source = TransactionSource.MANUAL),
        Transaction(id = 2, amount = 80.0, categoryId = 2, note = "Uber ride to office", timestamp = System.currentTimeMillis() - 43200000, source = TransactionSource.MANUAL),
        Transaction(id = 3, amount = 120.0, categoryId = 3, note = "Netflix monthly renewal", timestamp = System.currentTimeMillis() - 21600000, source = TransactionSource.ALERT, rawSms = "Paid Netflix EGP 120.00 from card x1234")
    ))
    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
    override suspend fun addTransaction(transaction: Transaction): Long = 0
    override suspend fun deleteTransaction(id: Long) {}
    override suspend fun getTransactionsByCategory(categoryId: Long, month: String): List<Transaction> = emptyList()
}

class MockBudgetRepository : BudgetRepository {
    override fun getBudgetsByMonth(month: String): Flow<List<Budget>> = flowOf(listOf(
        Budget(1, 1, 2000.0, month),
        Budget(2, 2, 500.0, month),
        Budget(3, 3, 300.0, month),
        Budget(4, 4, 8000.0, month)
    ))
    override suspend fun setBudget(budget: Budget): Long = 0
    override suspend fun deleteBudget(id: Long) {}
}

object PlayStoreTestFixtures {
    fun createSeededViewModel(context: Context): FinanceViewModel {
        return FinanceViewModel(
            categoryRepository = MockCategoryRepository(),
            transactionRepository = MockTransactionRepository(),
            budgetRepository = MockBudgetRepository(),
            context = context
        )
    }
}
