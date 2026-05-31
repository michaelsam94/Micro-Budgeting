package com.michael.microbudgeting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.michael.microbudgeting.presentation.FinanceViewModel
import com.michael.microbudgeting.presentation.MainScreen
import com.michael.microbudgeting.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels {
        FinanceViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
