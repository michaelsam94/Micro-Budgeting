package com.michael.microbudgeting.data.repository

import com.michael.microbudgeting.data.local.db.dao.BudgetDao
import com.michael.microbudgeting.data.local.db.dao.CategoryDao
import com.michael.microbudgeting.data.local.db.dao.TransactionDao
import com.michael.microbudgeting.data.local.db.entity.BudgetEntity
import com.michael.microbudgeting.data.local.db.entity.CategoryEntity
import com.michael.microbudgeting.data.local.db.entity.TransactionEntity
import com.michael.microbudgeting.domain.model.Budget
import com.michael.microbudgeting.domain.model.Category
import com.michael.microbudgeting.domain.model.Transaction
import com.michael.microbudgeting.domain.model.TransactionSource
import com.michael.microbudgeting.domain.repository.BudgetRepository
import com.michael.microbudgeting.domain.repository.CategoryRepository
import com.michael.microbudgeting.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.TimeZone

// Utility to parse "YYYY-MM" to start and end millisecond timestamps of that month
fun getMonthStartEndInterval(monthStr: String): Pair<Long, Long> {
    val parts = monthStr.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
    val month = (parts.getOrNull(1)?.toIntOrNull() ?: 5) - 1 // Calendar is 0-indexed

    val cal = Calendar.getInstance()
    cal.timeZone = TimeZone.getDefault()
    cal.clear()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis

    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    val end = cal.timeInMillis

    return Pair(start, end)
}

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addCategory(category: Category): Long {
        return categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun deleteCategory(id: Long) {
        categoryDao.deleteCategoryById(id)
    }

    override suspend fun insertDefaultCategoriesIfNeeded() {
        if (categoryDao.getCategoryCount() == 0) {
            val defaults = listOf(
                CategoryEntity(name = "Groceries", iconName = "shopping_cart", colorHex = "#4CAF50"),
                CategoryEntity(name = "Food & Dining", iconName = "restaurant", colorHex = "#FF9800"),
                CategoryEntity(name = "Transport", iconName = "directions_car", colorHex = "#2196F3"),
                CategoryEntity(name = "Health", iconName = "local_hospital", colorHex = "#E91E63"),
                CategoryEntity(name = "Utilities", iconName = "bolt", colorHex = "#9C27B0"),
                CategoryEntity(name = "Shopping", iconName = "local_mall", colorHex = "#00BCD4"),
                CategoryEntity(name = "Uncategorized", iconName = "category", colorHex = "#9E9E9E")
            )
            for (category in defaults) {
                categoryDao.insertCategory(category)
            }
        }
    }
}

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : TransactionRepository {

    override fun getTransactionsByMonth(month: String): Flow<List<Transaction>> {
        val interval = getMonthStartEndInterval(month)
        return categoryDao.getAllCategories().combine(
            transactionDao.getTransactionsBetween(interval.first, interval.second)
        ) { categories, transactionEntities ->
            val categoryMap = categories.associate { it.id to it.toDomain() }
            transactionEntities.map { entity ->
                entity.toDomain().copy(category = categoryMap[entity.categoryId])
            }
        }
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return categoryDao.getAllCategories().combine(
            transactionDao.getAllTransactions()
        ) { categories, transactionEntities ->
            val categoryMap = categories.associate { it.id to it.toDomain() }
            transactionEntities.map { entity ->
                entity.toDomain().copy(category = categoryMap[entity.categoryId])
            }
        }
    }

    override suspend fun addTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    override suspend fun getTransactionsByCategory(categoryId: Long, month: String): List<Transaction> {
        val interval = getMonthStartEndInterval(month)
        val entities = transactionDao.getTransactionsByCategoryForMonth(categoryId, interval.first, interval.second)
        val categoryEntity = categoryDao.getCategoryById(categoryId)
        val catDomain = categoryEntity?.toDomain()
        return entities.map { it.toDomain().copy(category = catDomain) }
    }
}

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getBudgetsByMonth(month: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonth(month).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun setBudget(budget: Budget): Long {
        return budgetDao.insertBudget(budget.toEntity())
    }

    override suspend fun deleteBudget(id: Long) {
        budgetDao.deleteBudgetById(id)
    }
}

// Extension Mapper functions
fun CategoryEntity.toDomain() = Category(id, name, iconName, colorHex)
fun Category.toEntity() = CategoryEntity(id, name, iconName, colorHex)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    amount = amount,
    categoryId = categoryId,
    note = note,
    timestamp = timestamp,
    source = if (source == "ALERT") TransactionSource.ALERT else TransactionSource.MANUAL,
    rawSms = rawSms
)
fun Transaction.toEntity() = TransactionEntity(
    id = id,
    amount = amount,
    categoryId = categoryId,
    note = note,
    timestamp = timestamp,
    source = source.name,
    rawSms = rawSms
)

fun BudgetEntity.toDomain() = Budget(id, categoryId, limitAmount, month)
fun Budget.toEntity() = BudgetEntity(id, categoryId, limitAmount, month)
