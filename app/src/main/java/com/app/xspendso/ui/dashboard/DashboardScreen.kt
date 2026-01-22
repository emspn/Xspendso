package com.app.xspendso.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.xspendso.data.TransactionEntity
import com.app.xspendso.ui.people.PeopleViewModel
import com.app.xspendso.ui.theme.AppBackground
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    peopleViewModel: PeopleViewModel,
    onProfileClick: () -> Unit,
    onPeopleClick: () -> Unit
) {
    var activeTab by remember { mutableStateOf("overview") }
    val transactions by viewModel.transactions.collectAsState()
    val monthlyAnalytics by viewModel.monthlyAnalytics.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val totalReceived by viewModel.totalReceived.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    val showManualPaymentSheet by viewModel.showManualPaymentSheet.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val peopleContacts by peopleViewModel.allContacts.collectAsState()
    
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    var selectedTransactionForEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

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
            peopleViewModel = peopleViewModel,
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
            },
            onSplit = { splits ->
                viewModel.splitExpenseWithMap(selectedTransactionForEdit!!, splits, peopleViewModel)
                selectedTransactionForEdit = null
            }
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Part 1: Non-sticky (Moves up when scrolling)
        item {
            Column(modifier = Modifier.statusBarsPadding()) {
                DashboardSummarySection(
                    totalSpent = totalSpent,
                    totalReceived = totalReceived,
                    isSyncing = isSyncing,
                    timeFilter = timeFilter,
                    currencyFormatter = currencyFormatter,
                    onProfileClick = onProfileClick
                )
            }
        }

        // Part 2: Sticky (Stays at the top when Part 1 is scrolled away)
        stickyHeader {
            Column(modifier = Modifier.background(AppBackground)) {
                DashboardSearchSection(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.onSearchQueryChange(it) }
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
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
            }
        }

        // Part 3: List Content
        when (activeTab) {
            "overview" -> {
                // 1. People Ledger Section (First)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "People Ledger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = onPeopleClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Manage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        onClick = onPeopleClick
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (peopleContacts.isEmpty()) {
                                Text(
                                    text = "No contacts added till now",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            } else {
                                val recentPeople = peopleContacts.take(3)
                                recentPeople.forEachIndexed { index, contact ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(contact.name, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text(
                                            text = if (contact.netBalance >= 0) "Receivable: ₹${String.format("%.2f", contact.netBalance)}" else "Payable: ₹${String.format("%.2f", Math.abs(contact.netBalance))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (contact.netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    if (index < recentPeople.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Export Transactions Section (Second)
                item { 
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        QuickActions(
                            isSyncing = isSyncing,
                            onSync = { viewModel.importTransactions() },
                            onExportPdf = { viewModel.exportTransactionsToPdf(context) },
                            onExportCsv = { viewModel.exportTransactionsToCsv(context) }
                        )
                    }
                }

                // 3. Recent Transactions Section (Third)
                if (transactions.isEmpty() && !isSyncing) {
                    item { Box(modifier = Modifier.padding(20.dp)) { EmptyState() } }
                } else if (transactions.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Button(
                                onClick = { 
                                    activeTab = "transactions" 
                                    scope.launch {
                                        listState.animateScrollToItem(1) 
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("View All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Combined card for last 5 transactions
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val recentTxs = transactions.take(5)
                                recentTxs.forEachIndexed { index, transaction ->
                                    TransactionItem(
                                        transaction = transaction,
                                        onLongClick = { selectedTransactionForEdit = transaction }
                                    )
                                    if (index < recentTxs.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                item { 
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        CategoryBreakdownSection(monthlyAnalytics, totalSpent)
                    }
                }
            }
            "transactions" -> {
                items(transactions, key = { "all_${it.id}" }) { transaction ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
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
