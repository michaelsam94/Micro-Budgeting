package com.michael.microbudgeting.presentation

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.michael.microbudgeting.data.sms.SmsParser
import com.michael.microbudgeting.domain.model.BudgetSummary
import com.michael.microbudgeting.domain.model.Category
import com.michael.microbudgeting.domain.model.Transaction
import com.michael.microbudgeting.domain.model.TransactionSource
import com.michael.microbudgeting.presentation.components.BudgetProgressBar
import com.michael.microbudgeting.presentation.components.CategoryBadge
import com.michael.microbudgeting.presentation.components.SpendingPieChart
import com.michael.microbudgeting.ui.theme.BrandGold
import com.michael.microbudgeting.ui.theme.BrandGoldDark
import com.michael.microbudgeting.ui.theme.BrandInk
import com.michael.microbudgeting.ui.theme.BrandSlate
import com.michael.microbudgeting.ui.theme.BrandTeal
import com.michael.microbudgeting.ui.theme.BrandTealDark
import com.michael.microbudgeting.ui.theme.BrandTealLight
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Setup Toasts
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearToastMessage()
        }
    }

    var selectedTab by remember { mutableStateOf(0) }

    // Dialog state controllers
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showConfigureBudgetDialog by remember { mutableStateOf(false) }
    var selectedBudgetCategory by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MicroBudgetingToolbarIcon(modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Micro Budgeting",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                actions = {
                    // Month Selector Dropdown
                    MonthSelectorMenu(
                        currentMonth = uiState.selectedMonth,
                        onMonthSelected = { viewModel.setMonth(it) }
                    )
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
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = if (selectedTab == 0) Icons.Default.Home else Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Overview") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = if (selectedTab == 1) Icons.Default.List else Icons.Default.List, contentDescription = "Transactions") },
                    label = { Text("Expenses") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { 
                        BadgedBox(badge = {
                            if (uiState.pendingSmsExpenses.isNotEmpty()) {
                                Badge { Text(uiState.pendingSmsExpenses.size.toString()) }
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Bank Alerts")
                        }
                    },
                    label = { Text("Alerts") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Backup") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddTransactionDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
                }
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
                    onConfigureBudget = { category ->
                        selectedBudgetCategory = category
                        showConfigureBudgetDialog = true
                    }
                )
                1 -> TransactionsTab(
                    transactions = uiState.transactions,
                    onDelete = { viewModel.deleteTransaction(it) }
                )
                2 -> SmsParseTab(
                    uiState = uiState,
                    onParseText = { viewModel.parseBankAlertText(it) },
                    onConfirm = { expense, categoryId, note -> 
                        viewModel.confirmSmsExpense(expense, categoryId, note)
                    },
                    onReject = { viewModel.rejectSmsExpense(it) }
                )
                3 -> SecurityBackupTab(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Configure Budget Dialog
    if (showConfigureBudgetDialog && selectedBudgetCategory != null) {
        val category = selectedBudgetCategory!!
        var budgetAmountStr by remember { mutableStateOf("") }
        
        Dialog(onDismissRequest = { showConfigureBudgetDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set Monthly Limit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Category: ${category.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = budgetAmountStr,
                        onValueChange = { budgetAmountStr = it },
                        label = { Text("Monthly Limit (EGP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showConfigureBudgetDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = budgetAmountStr.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0) {
                                    viewModel.setCategoryBudget(category.id, amt)
                                    showConfigureBudgetDialog = false
                                } else {
                                    Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showAddTransactionDialog) {
        var trAmountStr by remember { mutableStateOf("") }
        var trNote by remember { mutableStateOf("") }
        var selectedCategoryId by remember { mutableStateOf<Long?>(uiState.categories.getOrNull(0)?.id) }
        
        Dialog(onDismissRequest = { showAddTransactionDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            text = "Add Micro Expense",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    item {
                        OutlinedTextField(
                            value = trAmountStr,
                            onValueChange = { trAmountStr = it },
                            label = { Text("Expense Amount (EGP)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = trNote,
                            onValueChange = { trNote = it },
                            label = { Text("Details / Merchant Note") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    item {
                        Text(
                            text = "Choose Category",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )
                    }

                    // Display choices as scrollable chips
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.categories) { cat ->
                                val isSelected = cat.id == selectedCategoryId
                                val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                val scaleBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(scaleBg)
                                        .border(2.dp, borderCol, RoundedCornerShape(24.dp))
                                        .clickable { selectedCategoryId = cat.id }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryBadge(iconName = cat.iconName, hexColor = cat.colorHex, size = 20)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showAddTransactionDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val amt = trAmountStr.toDoubleOrNull() ?: 0.0
                                    val catId = selectedCategoryId
                                    if (amt > 0.0 && catId != null) {
                                        viewModel.addManualTransaction(
                                            amount = amt,
                                            categoryId = catId,
                                            note = trNote.ifEmpty { "Manual expense" },
                                            date = System.currentTimeMillis()
                                        )
                                        showAddTransactionDialog = false
                                    } else {
                                        Toast.makeText(context, "Check inputs.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MicroBudgetingToolbarIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val radius = w * 0.22f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(BrandTealLight, BrandTealDark),
                start = androidx.compose.ui.geometry.Offset.Zero,
                end = androidx.compose.ui.geometry.Offset(w, h)
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )
        val diagonal = Path().apply {
            moveTo(0f, h * 0.12f)
            lineTo(w, h * 0.55f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(diagonal, color = BrandTealDark.copy(alpha = 0.82f))

        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.24f, h * 0.18f),
            size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f)
        )
        drawRoundRect(
            color = BrandTealLight,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.32f, h * 0.28f),
            size = androidx.compose.ui.geometry.Size(w * 0.23f, h * 0.08f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f, w * 0.03f)
        )
        listOf(0.46f, 0.57f, 0.68f).forEachIndexed { index, rowY ->
            val rowColor = if (index == 0) BrandTeal else BrandSlate
            drawCircle(
                color = rowColor,
                radius = w * 0.028f,
                center = androidx.compose.ui.geometry.Offset(w * 0.31f, h * rowY)
            )
            drawRoundRect(
                color = rowColor,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.37f, h * rowY - h * 0.018f),
                size = androidx.compose.ui.geometry.Size(w * if (index == 1) 0.21f else 0.25f, h * 0.036f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f, w * 0.02f)
            )
        }
        drawCircle(
            color = BrandGoldDark,
            radius = w * 0.15f,
            center = androidx.compose.ui.geometry.Offset(w * 0.66f, h * 0.72f)
        )
        drawCircle(
            color = BrandGold,
            radius = w * 0.15f,
            center = androidx.compose.ui.geometry.Offset(w * 0.61f, h * 0.67f)
        )
        drawLine(
            color = BrandInk,
            start = androidx.compose.ui.geometry.Offset(w * 0.54f, h * 0.67f),
            end = androidx.compose.ui.geometry.Offset(w * 0.60f, h * 0.75f),
            strokeWidth = w * 0.05f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = BrandInk,
            start = androidx.compose.ui.geometry.Offset(w * 0.60f, h * 0.75f),
            end = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.58f),
            strokeWidth = w * 0.05f,
            cap = StrokeCap.Round
        )
    }
}

// ------------------- COMPOSABLE TABS -------------------

@Composable
fun DashboardTab(
    summaries: List<BudgetSummary>,
    onConfigureBudget: (Category) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Spending Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SpendingPieChart(summaries = summaries)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = "Budget Goals & Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (summaries.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Loading budgeting status...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(summaries, key = { it.category.id }) { summary ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        BudgetProgressBar(summary = summary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onConfigureBudget(summary.category) }
                            ) {
                                Text(
                                    text = if (summary.budget == null) "Set Goal" else "Edit Goal",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TransactionsTab(
    transactions: List<Transaction>,
    onDelete: (Long) -> Unit
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No expenses recorded this month",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap the '+' floating button to log one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
        items(transactions, key = { it.id }) { transaction ->
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            val formattedDate = sdf.format(Date(transaction.timestamp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        CategoryBadge(
                            iconName = transaction.category?.iconName ?: "category",
                            hexColor = transaction.category?.colorHex ?: "#9E9E9E",
                            size = 38
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = transaction.note,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (transaction.source == TransactionSource.ALERT) 
                                                MaterialTheme.colorScheme.secondaryContainer 
                                            else MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (transaction.source == TransactionSource.ALERT) "ALERT" else "MANUAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (transaction.source == TransactionSource.ALERT) 
                                            MaterialTheme.colorScheme.onSecondaryContainer 
                                        else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%,.1f EGP", transaction.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { onDelete(transaction.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SmsParseTab(
    uiState: FinanceUiState,
    onParseText: (String) -> Unit,
    onConfirm: (SmsParser.ParsedExpense, Long, String) -> Unit,
    onReject: (SmsParser.ParsedExpense) -> Unit
) {
    var alertText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Parse Bank Alert",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Paste a bank transaction alert to extract the amount and suggest a category. Micro Budgeting does not request inbox access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = alertText,
                    onValueChange = { alertText = it },
                    label = { Text("Bank alert text") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onParseText(alertText)
                        alertText = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Parse Alert")
                }
            }
        }

        if (uiState.pendingSmsExpenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No pending alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Parsed bank alerts will appear here for review before they become expenses.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            return@LazyColumn
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Parsed Bank Alerts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(uiState.pendingSmsExpenses) { expense ->
            var expandedCategorySelect by remember { mutableStateOf(false) }
            val resolvedCategory = uiState.categories.find { it.name == expense.suggestedCategory }
                ?: uiState.categories.find { it.name == "Uncategorized" }
                ?: uiState.categories.getOrNull(0)

            var selectedCategory by remember { mutableStateOf(resolvedCategory) }
            var editNoteText by remember { mutableStateOf(expense.merchant ?: "Bank alert expense") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%,.1f EGP", expense.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape)
                                .clickable { expandedCategorySelect = !expandedCategorySelect }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedCategory != null) {
                                    CategoryBadge(iconName = selectedCategory!!.iconName, hexColor = selectedCategory!!.colorHex, size = 18)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        selectedCategory!!.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    Text("Choose Category", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Interactive drop category chooser
                    AnimatedVisibility(visible = expandedCategorySelect) {
                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                "Map to category:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.categories) { cat ->
                                    val isChosen = cat.id == selectedCategory?.id
                                    val scaleCol = if (isChosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(scaleCol)
                                            .clickable { 
                                                selectedCategory = cat
                                                expandedCategorySelect = false
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CategoryBadge(cat.iconName, cat.colorHex, size = 14)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(cat.name, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = editNoteText,
                        onValueChange = { editNoteText = it },
                        label = { Text("Details note / Merchant") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Original alert: \"${expense.rawSms}\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onReject(expense) },
                            modifier = Modifier.weight(1f).height(44.dp).padding(end = 6.dp)
                        ) {
                            Text("Ignore", color = MaterialTheme.colorScheme.error)
                        }
                        
                        Button(
                            onClick = { 
                                selectedCategory?.let { cat ->
                                    onConfirm(expense, cat.id, editNoteText)
                                }
                            },
                            modifier = Modifier.weight(1.2f).height(44.dp).padding(start = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm Expense")
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SecurityBackupTab(
    viewModel: FinanceViewModel,
    uiState: FinanceUiState
) {
    var passwordExport by remember { mutableStateOf("") }
    var passwordImport by remember { mutableStateOf("") }
    var importBase64 by remember { mutableStateOf("") }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacy Sandbox Enforced",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "No Internet permission declared in manifest. Zero analytical scripts incorporated. Your finance logs are 100% offline, stored locally with AES-256 SQLCipher encryption.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Generate encrypted backup card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Encrypted Export",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Derives an secure AES key from a password using PBKDF2 to pack categories, goals and transactions. Restores anywhere.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    OutlinedTextField(
                        value = passwordExport,
                        onValueChange = { passwordExport = it },
                        label = { Text("Set Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (passwordExport.length >= 4) {
                                viewModel.exportEncryptedBackup(passwordExport)
                            } else {
                                Toast.makeText(context, "Password must be at least 4 characters.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Encrypt & Copy Backup")
                        }
                    }
                }
            }
        }

        // Restore encrypted backup card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Restore Decrypt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Paste a previously generated backup string token and type matching password to restore all offline logs.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = importBase64,
                        onValueChange = { importBase64 = it },
                        label = { Text("Paste Backup Token here") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = passwordImport,
                        onValueChange = { passwordImport = it },
                        label = { Text("Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (importBase64.isNotEmpty() && passwordImport.length >= 4) {
                                val ok = viewModel.importEncryptedBackup(importBase64, passwordImport)
                                if (ok) {
                                    importBase64 = ""
                                    passwordImport = ""
                                }
                            } else {
                                Toast.makeText(context, "Please paste token and input password.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Decrypt & Import Database")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ------------------- AUXILIARY COMPONENTS -------------------

@Composable
fun MonthSelectorMenu(
    currentMonth: String,
    onMonthSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    val dbFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    
    val displayMonth = try {
        val date = dbFormat.parse(currentMonth)
        if (date != null) displayFormat.format(date) else currentMonth
    } catch (e: Exception) {
        currentMonth
    }

    // Build selectable months list for year 2026
    val years = listOf("2026")
    val months = (1..12).map { String.format(Locale.US, "%02d", it) }
    val selectableMonths = mutableListOf<String>()
    for (y in years) {
        for (m in months) {
            selectableMonths.add("$y-$m")
        }
    }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(text = displayMonth, fontWeight = FontWeight.Bold) },
            trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Choose Month") }
        )
        
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            selectableMonths.forEach { m ->
                val date = dbFormat.parse(m)
                val label = if (date != null) displayFormat.format(date) else m
                
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onMonthSelected(m)
                        expanded = false
                    }
                )
            }
        }
    }
}
