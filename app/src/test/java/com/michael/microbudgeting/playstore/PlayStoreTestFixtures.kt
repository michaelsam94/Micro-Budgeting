package com.michael.microbudgeting.playstore

import android.content.Context
import com.michael.microbudgeting.domain.model.*
import com.michael.microbudgeting.domain.repository.*
import com.michael.microbudgeting.presentation.FinanceViewModel
import com.michael.microbudgeting.presentation.FinanceUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MockCategoryRepository : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> = flowOf(listOf(
        Category(1, "Groceries", "shopping_cart", "#0F766E"),
        Category(2, "Transport", "directions_car", "#155E63"),
        Category(3, "Food & Dining", "restaurant", "#F2B84B"),
        Category(4, "Shopping", "local_mall", "#12363A")
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
    private val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    private val categories = listOf(
        Category(1, "Groceries", "shopping_cart", "#0F766E"),
        Category(2, "Transport", "directions_car", "#155E63"),
        Category(3, "Food & Dining", "restaurant", "#F2B84B"),
        Category(4, "Shopping", "local_mall", "#12363A")
    )
    private val transactions = listOf(
        Transaction(id = 1, amount = 450.0, categoryId = 1, category = categories[0], note = "Weekly groceries purchase", timestamp = System.currentTimeMillis() - 86400000, source = TransactionSource.MANUAL),
        Transaction(id = 2, amount = 80.0, categoryId = 2, category = categories[1], note = "Uber ride to office", timestamp = System.currentTimeMillis() - 43200000, source = TransactionSource.MANUAL),
        Transaction(id = 3, amount = 250.0, categoryId = 3, category = categories[2], note = "Lunch and coffee", timestamp = System.currentTimeMillis() - 21600000, source = TransactionSource.ALERT, rawSms = "Paid restaurant EGP 250.00 from card x1234")
    )
    private val budgets = listOf(
        Budget(1, 1, 2000.0, month),
        Budget(2, 2, 500.0, month),
        Budget(3, 3, 900.0, month),
        Budget(4, 4, 1500.0, month)
    )

    fun createSeededViewModel(context: Context): FinanceViewModel {
        return FinanceViewModel(
            categoryRepository = MockCategoryRepository(),
            transactionRepository = MockTransactionRepository(),
            budgetRepository = MockBudgetRepository(),
            context = context
        )
    }

    fun seededUiState(): FinanceUiState {
        val summaries = categories.map { category ->
            val budget = budgets.firstOrNull { it.categoryId == category.id }
            val spent = transactions.filter { it.categoryId == category.id }.sumOf { it.amount }
            val limit = budget?.limitAmount ?: 0.0
            BudgetSummary(
                category = category,
                budget = budget,
                spent = spent,
                remaining = if (budget == null) 0.0 else limit - spent,
                percentUsed = if (limit == 0.0) 0f else (spent / limit).toFloat()
            )
        }
        return FinanceUiState(
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            budgetSummaries = summaries,
            selectedMonth = month
        )
    }
}
