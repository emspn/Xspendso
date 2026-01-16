package com.app.xspendso.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.xspendso.ui.dashboard.DashboardViewModel
import com.app.xspendso.ui.theme.*
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: DashboardViewModel
) {
    val transactions by viewModel.transactions.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val budgetStatus by viewModel.budgetStatus.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    // Analytics Calculation
    val categoryTotals = remember(transactions) {
        transactions.filter { it.type == "DEBIT" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> abs(tx.amount) } }
            .toList()
            .sortedByDescending { it.second }
    }

    if (showBudgetDialog) {
        BudgetDialog(
            currentLimit = budgetStatus?.totalLimit ?: 0.0,
            onDismiss = { showBudgetDialog = false },
            onConfirm = {
                viewModel.setMonthlyBudget(it)
                showBudgetDialog = false
            }
        )
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
                }) { Text("Apply", color = PrimarySteelBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.height(450.dp),
                title = { Text("Filter Range", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false
            )
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Spends Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    IconButton(onClick = { showDatePicker = true }, modifier = Modifier.background(AppSurface, CircleShape)) {
                        Icon(Icons.Default.CalendarMonth, null, tint = PrimarySteelBlue)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Utilization", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(currencyFormatter.format(totalSpent), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Button(
                        onClick = { showBudgetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimarySteelBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set Budget", fontSize = 13.sp)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Pie Chart Section
            item {
                SpendingPieChartCard(categoryTotals, totalSpent)
            }

            // 2. Breakdown List
            item {
                Text("Top Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            
            items(categoryTotals.take(6)) { (category, amount) ->
                CategoryCard(category, amount, totalSpent, currencyFormatter)
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun SpendingPieChartCard(data: List<Pair<String, Double>>, total: Double) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Expense Composition", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    val innerRadius = size.minDimension / 2.8f
                    
                    if (data.isEmpty()) {
                        drawCircle(color = Slate800, style = Stroke(width = 25.dp.toPx()))
                    } else {
                        data.forEach { (cat, amount) ->
                            val sweepAngle = (amount / total).toFloat() * 360f
                            drawArc(
                                color = getCategoryColor(cat),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 25.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${data.size}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Categories", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun CategoryCard(name: String, amount: Double, total: Double, formatter: NumberFormat) {
    val percentage = if (total > 0) (amount / total * 100).toInt() else 0
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(getCategoryColor(name), CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("$percentage% of total", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Text(formatter.format(amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun BudgetDialog(currentLimit: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toInt().toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Monthly Budget", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() }) amount = it },
                label = { Text("Limit (₹)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0) }, colors = ButtonDefaults.buttonColors(containerColor = PrimarySteelBlue)) {
                Text("Save", color = Color.Black)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun getCategoryColor(name: String): Color {
    return when (name) {
        "Food & Dining", "Groceries" -> Color(0xFFFF6B6B)
        "Bills & Utilities" -> Color(0xFF45B7D1)
        "Travel", "Cab & Rental" -> Color(0xFF4ECDC4)
        "Shopping" -> Color(0xFFFFA07A)
        "Health" -> Color(0xFFEC4899)
        "Investment" -> Color(0xFF10B981)
        else -> Color(0xFF64748B)
    }
}
