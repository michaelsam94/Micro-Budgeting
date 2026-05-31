package com.michael.microbudgeting.di

import android.content.Context
import com.michael.microbudgeting.data.local.db.AppDatabase
import com.michael.microbudgeting.data.repository.BudgetRepositoryImpl
import com.michael.microbudgeting.data.repository.CategoryRepositoryImpl
import com.michael.microbudgeting.data.repository.TransactionRepositoryImpl
import com.michael.microbudgeting.domain.repository.BudgetRepository
import com.michael.microbudgeting.domain.repository.CategoryRepository
import com.michael.microbudgeting.domain.repository.TransactionRepository

interface AppContainer {
    val categoryRepository: CategoryRepository
    val transactionRepository: TransactionRepository
    val budgetRepository: BudgetRepository
}

class AppContainerImpl(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao())
    }

    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database.transactionDao(), database.categoryDao())
    }

    override val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(database.budgetDao())
    }
}
