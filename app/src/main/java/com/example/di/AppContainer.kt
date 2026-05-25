package com.example.di

import android.content.Context
import com.example.data.local.db.AppDatabase
import com.example.data.repository.BudgetRepositoryImpl
import com.example.data.repository.CategoryRepositoryImpl
import com.example.data.repository.TransactionRepositoryImpl
import com.example.domain.repository.BudgetRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.TransactionRepository

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
