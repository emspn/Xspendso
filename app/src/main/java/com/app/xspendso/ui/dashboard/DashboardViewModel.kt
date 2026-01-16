package com.app.xspendso.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.xspendso.data.*
import com.app.xspendso.domain.TransactionRepository
import com.app.xspendso.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.round

enum class TimeFilter {
    TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM, ALL_TIME
}

class DashboardViewModel(
    private val repository: TransactionRepository,
    private val syncLedgerUseCase: SyncLedgerUseCase,
    private val getMonthlyAnalyticsUseCase: GetMonthlyAnalyticsUseCase,
    private val getBudgetingStatusUseCase: GetBudgetingStatusUseCase,
    private val predictMonthEndSavingsUseCase: PredictMonthEndSavingsUseCase,
    private val exportReportUseCase: ExportReportUseCase,
    private val getMerchantAnalyticsUseCase: GetMerchantAnalyticsUseCase,
    private val getBalanceHistoryUseCase: GetBalanceHistoryUseCase,
    private val getMonthOverMonthComparisonUseCase: GetMonthOverMonthComparisonUseCase,
    private val getAccountBreakdownUseCase: GetAccountBreakdownUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedType = MutableStateFlow<String?>(null)
    val selectedType: StateFlow<String?> = _selectedType.asStateFlow()

    private val _timeFilter = MutableStateFlow(TimeFilter.THIS_MONTH)
    val timeFilter: StateFlow<TimeFilter> = _timeFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _showManualPaymentSheet = MutableStateFlow(false)
    val showManualPaymentSheet: StateFlow<Boolean> = _showManualPaymentSheet.asStateFlow()

    private data class FilterState(
        val query: String,
        val category: String?,
        val type: String?,
        val time: TimeFilter,
        val customRange: Pair<Long, Long>?
    )

    private val filterState = combine(
        _searchQuery,
        _selectedCategory,
        _selectedType,
        _timeFilter,
        _customDateRange
    ) { query, category, type, time, customRange ->
        FilterState(query, category, type, time, customRange)
    }

    val transactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .combine(filterState) { txs, filters ->
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val startOfWeek = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val startOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val startOfYear = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            txs.filter { tx ->
                val matchesQuery = filters.query.isBlank() || 
                    tx.counterparty.contains(filters.query, ignoreCase = true) || 
                    tx.accountSource.contains(filters.query, ignoreCase = true) ||
                    tx.remark?.contains(filters.query, ignoreCase = true) == true
                
                val matchesCategory = filters.category == null || tx.category == filters.category
                val matchesType = filters.type == null || tx.type == filters.type
                
                val matchesTime = when (filters.time) {
                    TimeFilter.TODAY -> tx.timestamp >= startOfToday
                    TimeFilter.THIS_WEEK -> tx.timestamp >= startOfWeek
                    TimeFilter.THIS_MONTH -> tx.timestamp >= startOfMonth
                    TimeFilter.THIS_YEAR -> tx.timestamp >= startOfYear
                    TimeFilter.CUSTOM -> {
                        filters.customRange?.let { range ->
                            tx.timestamp >= range.first && tx.timestamp <= range.second
                        } ?: true
                    }
                    TimeFilter.ALL_TIME -> true
                }
                
                matchesQuery && matchesCategory && matchesType && matchesTime
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalSpent: StateFlow<Double> = transactions.map { txs ->
        txs.filter { it.type == "DEBIT" }.sumOf { abs(it.amount) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    private val currentMonthYear: String
        get() {
            val cal = Calendar.getInstance()
            return "${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.YEAR)}"
        }

    val currentBudget: StateFlow<BudgetEntity?> = repository.getBudgetForMonth(currentMonthYear)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val budgetStatus: StateFlow<BudgetStatus?> = combine(transactions, currentBudget) { txs, budget ->
        if (budget != null) getBudgetingStatusUseCase(txs, budget) else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savingsPrediction: StateFlow<SavingsPrediction?> = combine(transactions, currentBudget) { txs, budget ->
        if (budget != null) predictMonthEndSavingsUseCase(txs, budget.totalLimit) else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val monthlyAnalytics: StateFlow<Map<String, Double>> = transactions.map {
        getMonthlyAnalyticsUseCase(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val merchantAnalytics: StateFlow<List<MerchantAnalytics>> = transactions.map {
        getMerchantAnalyticsUseCase(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val balanceHistory: StateFlow<List<BalancePoint>> = transactions.map {
        getBalanceHistoryUseCase(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<GoalEntity>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthOverMonthComparison: StateFlow<List<ComparisonResult>> = transactions.map {
        getMonthOverMonthComparisonUseCase(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorizationRules: StateFlow<List<CategorizationRule>> = repository.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountBreakdown: StateFlow<List<AccountBreakdown>> = transactions.map {
        getAccountBreakdownUseCase(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategoryFilterChange(category: String?) {
        _selectedCategory.value = category
    }

    fun onTypeFilterChange(type: String?) {
        _selectedType.value = type
    }

    fun onTimeFilterChange(time: TimeFilter) {
        _timeFilter.value = time
    }

    fun onCustomDateRangeChange(start: Long, end: Long) {
        _customDateRange.value = start to end
        _timeFilter.value = TimeFilter.CUSTOM
    }

    fun setShowManualPaymentSheet(show: Boolean) {
        _showManualPaymentSheet.value = show
    }

    fun setMonthlyBudget(limit: Double) {
        viewModelScope.launch {
            val existing = currentBudget.value
            repository.insertBudget(
                BudgetEntity(
                    monthYear = currentMonthYear,
                    totalLimit = limit,
                    categoryLimits = existing?.categoryLimits ?: emptyMap()
                )
            )
        }
    }
    
    fun setCategoryBudget(category: String, limit: Double) {
        viewModelScope.launch {
            val existing = currentBudget.value ?: BudgetEntity(currentMonthYear, 0.0)
            val newLimits = existing.categoryLimits.toMutableMap()
            newLimits[category] = limit
            repository.insertBudget(existing.copy(categoryLimits = newLimits))
        }
    }

    fun importTransactions() {
        viewModelScope.launch {
            _isSyncing.value = true
            syncLedgerUseCase()
            _isSyncing.value = false
        }
    }

    fun forceReparseAllData(prefsManager: PrefsManager) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.deleteAllUserData()
            prefsManager.lastSmsSyncTimestamp = 0L
            syncLedgerUseCase()
            _isSyncing.value = false
        }
    }

    fun addManualTransaction(counterparty: String, amount: Double, category: String, type: String) {
        viewModelScope.launch {
            val transaction = TransactionEntity(
                accountSource = "Manual",
                counterparty = counterparty,
                category = category,
                amount = if (type == "DEBIT") -amount else amount,
                timestamp = System.currentTimeMillis(),
                method = "Manual",
                type = type,
                remark = "Manually added",
                enrichedSource = "USER"
            )
            repository.insertTransaction(transaction)
            _showManualPaymentSheet.value = false
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: TransactionEntity, newCounterparty: String, remark: String, newCategory: String, newAccountSource: String) {
        viewModelScope.launch {
            val updatedTx = transaction.copy(
                counterparty = newCounterparty,
                remark = remark,
                category = newCategory,
                accountSource = newAccountSource,
                enrichedSource = "USER"
            )
            repository.updateTransaction(updatedTx)
            
            val amountRounded = round(transaction.amount)
            val timeBucket = transaction.timestamp / (10 * 60 * 1000)
            
            repository.insertPattern(
                CorrectionPattern(
                    amountRounded = amountRounded,
                    timeBucket = timeBucket,
                    accountSource = newAccountSource,
                    correctedCounterparty = newCounterparty,
                    correctedCategory = newCategory
                )
            )
        }
    }

    fun addGoal(title: String, target: Double, deadline: Long? = null) {
        viewModelScope.launch {
            repository.insertGoal(GoalEntity(title = title, targetAmount = target, deadline = deadline))
        }
    }

    fun updateGoalProgress(goal: GoalEntity, saved: Double) {
        viewModelScope.launch {
            repository.updateGoal(goal.copy(savedAmount = saved))
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun addCategorizationRule(pattern: String, category: String) {
        viewModelScope.launch {
            repository.insertRule(CategorizationRule(pattern.lowercase(), category))
            reapplyCategorizationRules()
        }
    }

    fun deleteCategorizationRule(rule: CategorizationRule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
        }
    }

    private suspend fun reapplyCategorizationRules() {
        val allRules = repository.getAllRules().first()
        val allTxs = repository.getAllTransactions().first()
        
        val updatedTxs = allTxs.map { tx ->
            val matchingRule = allRules.find { tx.counterparty.contains(it.merchantPattern, ignoreCase = true) }
            if (matchingRule != null) {
                tx.copy(category = matchingRule.category, enrichedSource = "RULE")
            } else tx
        }
        
        repository.insertTransactions(updatedTxs)
    }

    fun exportTransactionsToPdf(context: Context) {
        viewModelScope.launch {
            val txs = transactions.value
            if (txs.isNotEmpty()) {
                val fileName = "Xpendso_Report_${System.currentTimeMillis()}.pdf"
                val file = exportReportUseCase.exportToPdf(txs, fileName)
                file?.let { shareFile(context, it, "application/pdf") }
            }
        }
    }
    
    fun exportTransactionsToCsv(context: Context) {
        viewModelScope.launch {
            val txs = transactions.value
            if (txs.isNotEmpty()) {
                val fileName = "Xpendso_Report_${System.currentTimeMillis()}.csv"
                val file = exportReportUseCase.exportToCsv(txs, fileName)
                file?.let { shareFile(context, it, "text/csv") }
            }
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }

    class Factory(
        private val repository: TransactionRepository,
        private val syncLedgerUseCase: SyncLedgerUseCase,
        private val getMonthlyAnalyticsUseCase: GetMonthlyAnalyticsUseCase,
        private val getBudgetingStatusUseCase: GetBudgetingStatusUseCase,
        private val predictMonthEndSavingsUseCase: PredictMonthEndSavingsUseCase,
        private val exportReportUseCase: ExportReportUseCase,
        private val getMerchantAnalyticsUseCase: GetMerchantAnalyticsUseCase,
        private val getBalanceHistoryUseCase: GetBalanceHistoryUseCase,
        private val getMonthOverMonthComparisonUseCase: GetMonthOverMonthComparisonUseCase,
        private val getAccountBreakdownUseCase: GetAccountBreakdownUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                repository, 
                syncLedgerUseCase, 
                getMonthlyAnalyticsUseCase,
                getBudgetingStatusUseCase,
                predictMonthEndSavingsUseCase,
                exportReportUseCase,
                getMerchantAnalyticsUseCase,
                getBalanceHistoryUseCase,
                getMonthOverMonthComparisonUseCase,
                getAccountBreakdownUseCase
            ) as T
        }
    }
}
