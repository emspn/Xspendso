package com.app.xspendso.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.xspendso.domain.usecase.*
import com.app.xspendso.ui.dashboard.DashboardViewModel
import com.app.xspendso.ui.theme.*
import java.text.NumberFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: DashboardViewModel,
    onNavigateToPlanner: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val budgetStatus by viewModel.budgetStatus.collectAsState()
    val savingsPrediction by viewModel.savingsPrediction.collectAsState()
    val merchantAnalytics by viewModel.merchantAnalytics.collectAsState()
    val momComparison by viewModel.monthOverMonthComparison.collectAsState()
    val spendingTrends by viewModel.spendingTrends.collectAsState()
    val accountBreakdown by viewModel.accountBreakdown.collectAsState()
    val dayOfWeekSpend by viewModel.dayOfWeekSpend.collectAsState()
    
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    val categoryTotals = remember(transactions) {
        transactions.filter { it.type == "DEBIT" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> abs(tx.amount) } }
            .toList()
            .sortedByDescending { it.second }
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

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Human-crafted Header
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Intelligence", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                IconButton(onClick = { showDatePicker = true }, modifier = Modifier.background(AppSurface, CircleShape)) {
                    Icon(Icons.Default.CalendarMonth, null, tint = PrimarySteelBlue)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Budget vs Reality
            item {
                budgetStatus?.let { status ->
                    BudgetRealityCard(status, currencyFormatter, onNavigateToPlanner)
                } ?: run {
                    // Empty state for budget
                    Surface(
                        color = AppSurface,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clickable { onNavigateToPlanner() },
                        border = BorderStroke(1.dp, GlassWhite)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddChart, null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Budget Set", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Tap to plan your monthly expenses", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }

            // 2. Prediction Hero Card
            item {
                AnimatedVisibility(
                    visible = savingsPrediction != null,
                    enter = fadeIn() + expandVertically()
                ) {
                    savingsPrediction?.let { 
                        PredictionCard(it, currencyFormatter)
                    }
                }
            }

            // 3. Category-wise Budget Alerts
            item {
                budgetStatus?.let { status ->
                    val highUsageCategories = status.categoryStatus.filter { it.value.percentageUsed > 0.8f }
                    if (highUsageCategories.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Text("Budget Alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = ColorWarning)
                            Spacer(modifier = Modifier.height(12.dp))
                            highUsageCategories.forEach { (cat, catStatus) ->
                                CategoryAlertItem(cat, catStatus, currencyFormatter)
                            }
                        }
                    }
                }
            }

            // 4. Spending Heatmap (Day of Week Analysis)
            item {
                DayOfWeekHeatmapCard(dayOfWeekSpend)
            }

            // 5. Spending Trend Chart
            item {
                SpendingTrendChartCard(spendingTrends)
            }

            // 6. Category Breakdown
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("Top Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    SpendingPieChartCard(categoryTotals, totalSpent)
                }
            }

            // 7. Month over Month Comparison
            item {
                MoMComparisonSection(momComparison, currencyFormatter)
            }

            // 8. Account Breakdown
            item {
                AccountBreakdownSection(accountBreakdown, currencyFormatter)
            }

            // 9. Merchant Insights
            item {
                MerchantAnalyticsSection(merchantAnalytics.take(5), currencyFormatter)
            }
            
            // 10. Detailed Category List
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("Categorical Spending", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            items(categoryTotals) { (category, amount) ->
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    CategoryCard(category, amount, totalSpent, currencyFormatter)
                }
            }
        }
    }
}

@Composable
fun CategoryAlertItem(category: String, status: CategoryBudgetStatus, formatter: NumberFormat) {
    Surface(
        color = ColorWarning.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = BorderStroke(1.dp, ColorWarning.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = ColorWarning, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${(status.percentageUsed * 100).toInt()}% limit reached", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Text(formatter.format(status.remaining) + " left", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun BudgetRealityCard(status: BudgetStatus, formatter: NumberFormat, onClick: () -> Unit) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clickable { onClick() },
        border = BorderStroke(1.dp, GlassWhite)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Monthly Budget Target", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Icon(Icons.Default.Edit, null, tint = PrimarySteelBlue, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(formatter.format(status.totalSpent), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("/ ${formatter.format(status.totalLimit)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val progress = status.percentageUsed.coerceIn(0f, 1f)
            val barColor = when {
                progress > 0.9f -> ColorError
                progress > 0.7f -> ColorWarning
                else -> SecondaryEmerald
            }
            
            Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(Slate800)) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(barColor))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                if (status.remaining > 0) "${formatter.format(status.remaining)} remaining" else "Budget exceeded by ${formatter.format(abs(status.totalLimit - status.totalSpent))}",
                style = MaterialTheme.typography.labelMedium,
                color = if (status.remaining > 0) TextSecondary else ColorError
            )
        }
    }
}

@Composable
fun DayOfWeekHeatmapCard(daySpend: List<DayOfWeekSpend>) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Spending Habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("Which days are heaviest?", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                val maxAmount = daySpend.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
                daySpend.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        val barHeight = (day.amount / maxAmount * 80).dp
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (day.amount == maxAmount) PrimarySteelBlue else Slate700)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(day.dayName, style = MaterialTheme.typography.labelSmall, color = if (day.amount == maxAmount) TextPrimary else TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun PredictionCard(prediction: SavingsPrediction, formatter: NumberFormat) {
    val statusColor = when (prediction.status) {
        "SAFE" -> SecondaryEmerald
        "WARNING" -> ColorWarning
        else -> ColorError
    }

    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        border = BorderStroke(1.dp, GlassWhite)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when(prediction.status) {
                        "SAFE" -> "On Track"
                        "WARNING" -> "Approaching Limit"
                        else -> "Over Budget"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Predicted month-end spend: ${formatter.format(prediction.predictedSpent)}",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                "Daily allowance to stay in budget: ${formatter.format(prediction.dailyBudget)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = (prediction.currentSpent / prediction.predictedSpent.coerceAtLeast(1.0)).toFloat(),
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = statusColor,
                trackColor = Slate800
            )
        }
    }
}

@Composable
fun SpendingTrendChartCard(trends: List<DailyTrend>) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Spending Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(24.dp))
            
            if (trends.isEmpty()) {
                Box(modifier = Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No data available", color = TextSecondary)
                }
            } else {
                Box(modifier = Modifier.height(150.dp).fillMaxWidth()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxAmount = trends.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
                        val width = size.width
                        val height = size.height
                        val spacing = width / (trends.size.coerceAtLeast(2) - 1).coerceAtLeast(1)
                        
                        val path = Path()
                        trends.forEachIndexed { index, trend ->
                            val x = index * spacing
                            val y = height - (trend.amount / maxAmount * height).toFloat()
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        
                        drawPath(
                            path = path,
                            color = PrimarySteelBlue,
                            style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                        
                        // Points
                        trends.forEachIndexed { index, trend ->
                            val x = index * spacing
                            val y = height - (trend.amount / maxAmount * height).toFloat()
                            drawCircle(color = PrimarySteelBlue, radius = 4.dp.toPx(), center = Offset(x, y))
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    trends.firstOrNull()?.let { Text(it.dateLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                    trends.lastOrNull()?.let { Text(it.dateLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                }
            }
        }
    }
}

@Composable
fun MoMComparisonSection(comparisons: List<ComparisonResult>, formatter: NumberFormat) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Month-over-Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(comparisons.filter { it.currentMonthAmount > 0 || it.previousMonthAmount > 0 }.take(8)) { item ->
                Surface(
                    color = AppCard,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.width(160.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(item.category, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formatter.format(item.currentMonthAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        
                        val isUp = item.percentageChange > 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isUp) ColorError else SecondaryEmerald
                            )
                            Text(
                                "${abs(item.percentageChange).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUp) ColorError else SecondaryEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountBreakdownSection(accounts: List<AccountBreakdown>, formatter: NumberFormat) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Account Usage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        accounts.forEach { account ->
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                border = BorderStroke(1.dp, GlassWhite)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = PrimarySteelBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.accountName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Spent: ${formatter.format(account.totalSpent)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    account.latestBalance?.let {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Balance", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(formatter.format(it), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SecondaryEmerald)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MerchantAnalyticsSection(merchants: List<MerchantAnalytics>, formatter: NumberFormat) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Frequent Merchants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        merchants.forEach { merchant ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Slate800
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(merchant.merchantName.take(1).uppercase(), color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(merchant.merchantName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("${merchant.transactionCount} transactions • ${merchant.category}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Text(formatter.format(merchant.totalSpent), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
    }
}

@Composable
fun SpendingPieChartCard(data: List<Pair<String, Double>>, total: Double) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    if (data.isEmpty()) {
                        drawCircle(color = Slate800, style = Stroke(width = 20.dp.toPx()))
                    } else {
                        data.forEach { (cat, amount) ->
                            val sweepAngle = (amount / total).toFloat() * 360f
                            drawArc(
                                color = getCategoryColor(cat),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 20.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
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
        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
