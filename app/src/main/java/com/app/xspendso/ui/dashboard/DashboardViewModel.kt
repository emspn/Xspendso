package com.app.xspendso.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.xspendso.data.TransactionEntity
import com.app.xspendso.domain.TransactionRepository
import com.app.xspendso.domain.usecase.GetMonthlyAnalyticsUseCase
import com.app.xspendso.domain.usecase.ImportSmsUseCase
import com.app.xspendso.sms.SmsReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: TransactionRepository,
    private val importSmsUseCase: ImportSmsUseCase,
    private val getMonthlyAnalyticsUseCase: GetMonthlyAnalyticsUseCase
) : ViewModel() {

    val transactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlyAnalytics: StateFlow<Map<String, Double>> = transactions.map {
        getMonthlyAnalyticsUseCase(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val totalSpent: StateFlow<Double> = monthlyAnalytics.map { analytics ->
        analytics.values.sum()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun importTransactions() {
        viewModelScope.launch {
            importSmsUseCase()
        }
    }

    class Factory(
        private val repository: TransactionRepository,
        private val importSmsUseCase: ImportSmsUseCase,
        private val getMonthlyAnalyticsUseCase: GetMonthlyAnalyticsUseCase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository, importSmsUseCase, getMonthlyAnalyticsUseCase) as T
        }
    }
}