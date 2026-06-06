package com.michael.microbudgeting.playstore

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.michael.microbudgeting.data.sms.SmsParser
import com.michael.microbudgeting.presentation.DashboardTab
import com.michael.microbudgeting.presentation.FinanceUiState
import com.michael.microbudgeting.presentation.FinanceViewModel
import com.michael.microbudgeting.presentation.SecurityBackupTab
import com.michael.microbudgeting.presentation.SmsParseTab
import com.michael.microbudgeting.presentation.TransactionsTab
import com.michael.microbudgeting.ui.theme.MyApplicationTheme
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Category(PlayStoreScreenshotTests::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlayStoreScreenshotTest {

    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PlayStoreScreenShell(selectedTab: Int, uiState: FinanceUiState, viewModel: FinanceViewModel) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayStoreBrandMark(modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Micro Budgeting",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = {},
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Overview") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = {},
                        icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Transactions") },
                        label = { Text("Expenses") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = {},
                        icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Bank Alerts") },
                        label = { Text("Alerts") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = {},
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Backup") }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (selectedTab) {
                    0 -> DashboardTab(
                        summaries = uiState.budgetSummaries,
                        onConfigureBudget = {}
                    )
                    1 -> TransactionsTab(
                        transactions = uiState.transactions,
                        onDelete = {}
                    )
                    2 -> SmsParseTab(
                        uiState = uiState,
                        onParseText = {},
                        onConfirm = { _, _, _ -> },
                        onReject = {}
                    )
                    3 -> SecurityBackupTab(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_01_dashboard() {
        val viewModel = PlayStoreTestFixtures.createSeededViewModel(app)
        val uiState = PlayStoreTestFixtures.seededUiState()
        capturePlayStoreImage("phone/01_dashboard.png") {
            MyApplicationTheme {
                PlayStoreScreenShell(0, uiState, viewModel)
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_02_expenses() {
        val viewModel = PlayStoreTestFixtures.createSeededViewModel(app)
        val uiState = PlayStoreTestFixtures.seededUiState()
        capturePlayStoreImage("phone/02_expenses.png") {
            MyApplicationTheme {
                PlayStoreScreenShell(1, uiState, viewModel)
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_03_sms_parse() {
        val viewModel = PlayStoreTestFixtures.createSeededViewModel(app)
        val uiState = PlayStoreTestFixtures.seededUiState().copy(
            pendingSmsExpenses = listOf(
                SmsParser.ParsedExpense(250.0, "Fawry", "Paid Fawry bill EGP 250.00", "Utilities"),
                SmsParser.ParsedExpense(1200.0, "Carrefour", "Purchased items at Carrefour EGP 1,200.00", "Groceries")
            )
        )
        capturePlayStoreImage("phone/03_sms_parse.png") {
            MyApplicationTheme {
                PlayStoreScreenShell(2, uiState, viewModel)
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun phone_04_backup() {
        val viewModel = PlayStoreTestFixtures.createSeededViewModel(app)
        val uiState = PlayStoreTestFixtures.seededUiState()
        capturePlayStoreImage("phone/04_backup.png") {
            MyApplicationTheme {
                PlayStoreScreenShell(3, uiState, viewModel)
            }
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-xhdpi")
    fun tablet_01_dashboard() {
        val viewModel = PlayStoreTestFixtures.createSeededViewModel(app)
        val uiState = PlayStoreTestFixtures.seededUiState()
        capturePlayStoreImage("tablet/01_dashboard.png") {
            MyApplicationTheme {
                PlayStoreScreenShell(0, uiState, viewModel)
            }
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-xhdpi")
    fun tablet_02_expenses() {
        val viewModel = PlayStoreTestFixtures.createSeededViewModel(app)
        val uiState = PlayStoreTestFixtures.seededUiState()
        capturePlayStoreImage("tablet/02_expenses.png") {
            MyApplicationTheme {
                PlayStoreScreenShell(1, uiState, viewModel)
            }
        }
    }
}
