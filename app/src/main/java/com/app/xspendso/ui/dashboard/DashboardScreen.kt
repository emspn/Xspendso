package com.app.xspendso.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.xspendso.data.TransactionEntity
import com.app.xspendso.ui.theme.AppBackground
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    var activeTab by remember { mutableStateOf("overview") }
    val transactions by viewModel.transactions.collectAsState()
    val monthlyAnalytics by viewModel.monthlyAnalytics.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    val showManualPaymentSheet by viewModel.showManualPaymentSheet.collectAsState()

    var selectedTransactionForEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    LaunchedEffect(Unit) {
        viewModel.importTransactions()
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        viewModel.onCustomDateRangeChange(start, end)
                    }
                    showDatePicker = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.height(400.dp),
                title = { Text("Select Range", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false
            )
        }
    }

    if (showManualPaymentSheet) {
        ManualPaymentSheet(
            onDismiss = { viewModel.setShowManualPaymentSheet(false) },
            onSave = { counterparty, amount, category, type ->
                viewModel.addManualTransaction(counterparty, amount, category, type)
            }
        )
    }

    if (selectedTransactionForEdit != null) {
        TransactionEditSheet(
            transaction = selectedTransactionForEdit!!,
            onDismiss = { selectedTransactionForEdit = null },
            onSave = { counterparty, remark, category, accountSource ->
                viewModel.updateTransaction(
                    selectedTransactionForEdit!!, 
                    counterparty, 
                    remark, 
                    category,
                    accountSource
                )
                selectedTransactionForEdit = null
            },
            onDelete = {
                viewModel.deleteTransaction(selectedTransactionForEdit!!)
                selectedTransactionForEdit = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TimeFilterDropdown(
                selectedFilter = timeFilter,
                onFilterSelected = { viewModel.onTimeFilterChange(it) }
            )
            
            IconButton(
                onClick = { showDatePicker = true },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.DateRange, contentDescription = "Custom Range")
            }
        }

        TabRow(
            selectedTabIndex = if (activeTab == "overview") 0 else 1,
            containerColor = AppBackground,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }
        ) {
            Tab(
                selected = activeTab == "overview", 
                onClick = { activeTab = "overview" }, 
                text = { Text("Overview", style = MaterialTheme.typography.bodyMedium) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Tab(
                selected = activeTab == "transactions", 
                onClick = { activeTab = "transactions" }, 
                text = { Text("Transactions", style = MaterialTheme.typography.bodyMedium) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            when (activeTab) {
                "overview" -> {
                    item { 
                        QuickActions(
                            onSync = { viewModel.importTransactions() },
                            onExportPdf = { viewModel.exportTransactionsToPdf(context) },
                            onExportCsv = { viewModel.exportTransactionsToCsv(context) }
                        ) 
                    }
                    if (transactions.isEmpty() && !isSyncing) {
                        item { EmptyState() }
                    }
                    items(transactions.take(10), key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onLongClick = { selectedTransactionForEdit = transaction }
                        )
                    }
                    item { CategoryBreakdownSection(monthlyAnalytics, totalSpent) }
                }
                "transactions" -> {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onLongClick = { selectedTransactionForEdit = transaction }
                        )
                    }
                }
            }
        }
    }
}
