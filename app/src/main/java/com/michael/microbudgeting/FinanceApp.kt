package com.michael.microbudgeting

import android.app.Application
import com.michael.microbudgeting.di.AppContainer
import com.michael.microbudgeting.di.AppContainerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FinanceApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)

        // Seed default categories asynchronously on startup
        CoroutineScope(Dispatchers.IO).launch {
            container.categoryRepository.insertDefaultCategoriesIfNeeded()
        }
    }
}
