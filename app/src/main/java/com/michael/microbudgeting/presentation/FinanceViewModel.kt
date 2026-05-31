package com.michael.microbudgeting.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.michael.microbudgeting.FinanceApp
import com.michael.microbudgeting.data.backup.BackupBudget
import com.michael.microbudgeting.data.backup.BackupCategory
import com.michael.microbudgeting.data.backup.BackupPayload
import com.michael.microbudgeting.data.backup.BackupTransaction
import com.michael.microbudgeting.data.backup.EncryptedBackupSerializer
import com.michael.microbudgeting.data.local.db.entity.BudgetEntity
import com.michael.microbudgeting.data.local.db.entity.CategoryEntity
import com.michael.microbudgeting.data.local.db.entity.TransactionEntity
import com.michael.microbudgeting.data.repository.toEntity
import com.michael.microbudgeting.data.sms.SmsParser
import com.michael.microbudgeting.domain.model.Budget
import com.michael.microbudgeting.domain.model.BudgetSummary
import com.michael.microbudgeting.domain.model.Category
import com.michael.microbudgeting.domain.model.Transaction
import com.michael.microbudgeting.domain.model.TransactionSource
import com.michael.microbudgeting.domain.repository.BudgetRepository
import com.michael.microbudgeting.domain.repository.CategoryRepository
import com.michael.microbudgeting.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class FinanceUiState(
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val budgetSummaries: List<BudgetSummary> = emptyList(),
    val selectedMonth: String = "", // Formatted as "YYYY-MM"
    val pendingSmsExpenses: List<SmsParser.ParsedExpense> = emptyList(),
    val isLoading: Boolean = false,
    val toastMessage: String? = null
)

class FinanceViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private val selectedMonthFlow = MutableStateFlow("")

    init {
        // Compute default selected month (e.g. current year and month "YYYY-MM")
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val defaultMonth = sdf.format(Date())
        selectedMonthFlow.value = defaultMonth
        _uiState.update { it.copy(selectedMonth = defaultMonth) }

        // Start observing repository changes reactively
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            selectedMonthFlow.flatMapLatest { month ->
                combine(
                    categoryRepository.getAllCategories(),
                    transactionRepository.getTransactionsByMonth(month),
                    budgetRepository.getBudgetsByMonth(month)
                ) { categories, transactions, budgets ->
                    Triple(categories, transactions, budgets)
                }
            }.collectOnWorker { (categories, transactions, budgets) ->
                // Compute budget summaries
                val budgetMap = budgets.associateBy { it.categoryId }
                val spentMap = transactions.groupBy { it.categoryId }.mapValues { entry ->
                    entry.value.sumOf { it.amount }
                }

                // Create summaries for all categories
                val summaries = categories.map { category ->
                    val budget = budgetMap[category.id]
                    val spent = spentMap[category.id] ?: 0.0
                    val limit = budget?.limitAmount ?: 0.0
                    val remaining = if (budget == null) 0.0 else limit - spent
                    val percentUsed = if (limit == 0.0) 0f else (spent / limit).toFloat()

                    BudgetSummary(
                        category = category,
                        budget = budget,
                        spent = spent,
                        remaining = remaining,
                        percentUsed = percentUsed
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        categories = categories,
                        transactions = transactions,
                        budgets = budgets,
                        budgetSummaries = summaries
                    )
                }
            }
        }
    }

    fun setMonth(month: String) {
        selectedMonthFlow.value = month
        _uiState.update { it.copy(selectedMonth = month) }
    }

    fun addManualTransaction(amount: Double, categoryId: Long, note: String, date: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val transaction = Transaction(
                amount = amount,
                categoryId = categoryId,
                note = note,
                timestamp = date,
                source = TransactionSource.MANUAL
            )
            transactionRepository.addTransaction(transaction)
            showToast("Manual transaction saved.")
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionRepository.deleteTransaction(id)
            showToast("Transaction removed.")
        }
    }

    fun setCategoryBudget(categoryId: Long, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val budget = Budget(
                categoryId = categoryId,
                limitAmount = amount,
                month = selectedMonthFlow.value
            )
            budgetRepository.setBudget(budget)
            showToast("Budget configured successfully.")
        }
    }

    fun parseBankAlertText(alertText: String) {
        val normalizedText = alertText.trim()
        if (normalizedText.isEmpty()) {
            showToast("Paste a bank alert message first.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val existingSmsTexts = transactionRepository.getAllTransactions().first()
                    .filter { it.source == TransactionSource.ALERT }
                    .mapNotNull { it.rawSms }
                    .toSet()

                val parsed = SmsParser.parse(normalizedText)
                when {
                    parsed == null -> {
                        _uiState.update { it.copy(isLoading = false) }
                        showToast("Could not recognize an expense amount in that bank alert.")
                    }
                    existingSmsTexts.contains(parsed.rawSms) ||
                        _uiState.value.pendingSmsExpenses.any { it.rawSms == parsed.rawSms } -> {
                        _uiState.update { it.copy(isLoading = false) }
                        showToast("That bank alert is already pending or saved.")
                    }
                    else -> {
                        _uiState.update {
                            it.copy(
                                pendingSmsExpenses = listOf(parsed) + it.pendingSmsExpenses,
                                isLoading = false
                            )
                        }
                        showToast("Bank alert parsed. Review and confirm it below.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                showToast("Failed to parse alert: ${e.localizedMessage}")
            }
        }
    }

    fun confirmSmsExpense(expense: SmsParser.ParsedExpense, confirmedCategoryId: Long, actualNote: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val transaction = Transaction(
                amount = expense.amount,
                categoryId = confirmedCategoryId,
                note = actualNote,
                timestamp = System.currentTimeMillis(),
                source = TransactionSource.ALERT,
                rawSms = expense.rawSms
            )
            transactionRepository.addTransaction(transaction)
            
            // Remove from the pending view list
            _uiState.update { state ->
                state.copy(pendingSmsExpenses = state.pendingSmsExpenses.filter { it.rawSms != expense.rawSms })
            }
            showToast("Bank alert transaction confirmed and added.")
        }
    }

    fun rejectSmsExpense(expense: SmsParser.ParsedExpense) {
        _uiState.update { state ->
            state.copy(pendingSmsExpenses = state.pendingSmsExpenses.filter { it.rawSms != expense.rawSms })
        }
        showToast("Bank alert ignored.")
    }

    // Encrypted Export & Import
    fun exportEncryptedBackup(password: String): String? {
        if (password.length < 4) {
            showToast("Password must be at least 4 characters.")
            return null
        }
        _uiState.update { it.copy(isLoading = true) }
        
        try {
            // Read database state
            var bPayload: BackupPayload? = null
            
            // Collect latest room state safely
            val categories = _uiState.value.categories.map { BackupCategory(it.id, it.name, it.iconName, it.colorHex) }
            val budgets = _uiState.value.budgets.map { BackupBudget(it.id, it.categoryId, it.limitAmount, it.month) }
            val transactions = _uiState.value.transactions.map { BackupTransaction(it.id, it.amount, it.categoryId, it.note, it.timestamp, it.source.name, it.rawSms) }
            
            bPayload = BackupPayload(
                exportedAt = System.currentTimeMillis(),
                categories = categories,
                budgets = budgets,
                transactions = transactions
            )

            val serializer = EncryptedBackupSerializer()
            val ciphertextBytes = serializer.exportEncrypted(bPayload, password)
            
            // Base64 encode for simple string copy-paste porting
            val base64Str = Base64.encodeToString(ciphertextBytes, Base64.DEFAULT)

            // Save backing local file inside app's secure files directory as an additional file
            val file = File(context.filesDir, "micro_backup_${System.currentTimeMillis()}.fbak")
            file.writeBytes(ciphertextBytes)

            _uiState.update { it.copy(isLoading = false) }
            showToast("Backup generated and copied to Clipboard!")
            
            // Copy to clipboard automatically for convenient backup sharing
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Encrypted Finance Backup", base64Str)
            clipboard.setPrimaryClip(clip)

            return base64Str
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
            showToast("Backup failed: ${e.localizedMessage}")
            return null
        }
    }

    fun importEncryptedBackup(base64Str: String, password: String): Boolean {
        if (password.length < 4) {
            showToast("Invalid password.")
            return false
        }
        _uiState.update { it.copy(isLoading = true) }

        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            val serializer = EncryptedBackupSerializer()
            val payload = serializer.importEncrypted(decodedBytes, password)

            viewModelScope.launch(Dispatchers.IO) {
                // Restore backup payload by feeding database
                // Restore Categories
                for (cat in payload.categories) {
                    categoryRepository.addCategory(Category(cat.id, cat.name, cat.iconName, cat.colorHex))
                }
                // Restore Budgets
                for (b in payload.budgets) {
                    budgetRepository.setBudget(Budget(b.id, b.categoryId, b.limitAmount, b.month))
                }
                // Restore Transactions
                for (t in payload.transactions) {
                    transactionRepository.addTransaction(Transaction(
                        id = t.id,
                        amount = t.amount,
                        categoryId = t.categoryId,
                        note = t.note,
                        timestamp = t.timestamp,
                        source = if (t.source == "ALERT") TransactionSource.ALERT else TransactionSource.MANUAL,
                        rawSms = t.rawSms
                    ))
                }
                
                launch(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = false) }
                    showToast("Backup restored to Room successfully!")
                }
            }
            true
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false) }
            showToast("Failed to restore backup: Decryption mismatch or corrupt file.")
            false
        }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    // Custom flow collector running on a background worker thread
    private fun <T> Flow<T>.collectOnWorker(action: suspend (T) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            collect { action(it) }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext as FinanceApp
            val container = app.container
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(
                container.categoryRepository,
                container.transactionRepository,
                container.budgetRepository,
                context.applicationContext
            ) as T
        }
    }
}
