package com.app.xspendso.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.xspendso.domain.usecase.*
import com.app.xspendso.ui.dashboard.DashboardViewModel
import com.app.xspendso.ui.dashboard.TimeFilter
import com.app.xspendso.ui.theme.*
import java.text.NumberFormat
import java.util.*
import kotlin.math.*

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
    val spendingTrends by viewModel.spendingTrends.collectAsState()
    val accountBreakdown by viewModel.accountBreakdown.collectAsState()
    val dayOfWeekSpend by viewModel.dayOfWeekSpend.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
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
        // Header
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        AssistChip(
                            onClick = { showFilterMenu = true },
                            label = { Text(timeFilter.name.replace("_", " ").lowercase().capitalize()) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = TextPrimary,
                                containerColor = AppSurface
                            ),
                            border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.1f))
                        )
                        
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            modifier = Modifier.background(AppSurface)
                        ) {
                            TimeFilter.values().forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter.name.replace("_", " ").lowercase().capitalize(), color = TextPrimary) },
                                    onClick = {
                                        if (filter == TimeFilter.CUSTOM) {
                                            showDatePicker = true
                                        } else {
                                            viewModel.onTimeFilterChange(filter)
                                        }
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(AppSurface, CircleShape)
                            .border(1.dp, GlassWhite.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth, 
                            contentDescription = "Custom Date",
                            tint = PrimarySteelBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Monthly Budget Card
            item {
                budgetStatus?.let { status ->
                    BudgetRealityCard(status, savingsPrediction?.dailyBudget, currencyFormatter, onNavigateToPlanner)
                } ?: run {
                    Surface(
                        color = AppSurface,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clickable { onNavigateToPlanner() },
                        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddChart, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No Budget Set", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Plan your monthly expenses to get deeper insights", style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onNavigateToPlanner, colors = ButtonDefaults.buttonColors(containerColor = PrimarySteelBlue)) {
                                Text("Set Budget", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Savings Prediction
            item {
                AnimatedVisibility(
                    visible = savingsPrediction != null && timeFilter == TimeFilter.THIS_MONTH,
                    enter = fadeIn() + expandVertically()
                ) {
                    savingsPrediction?.let { 
                        PredictionCard(it, currencyFormatter)
                    }
                }
            }

            // 3. Top Categories (Interacting Pie Chart)
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("Top Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    SpendingPieChartCard(categoryTotals, totalSpent)
                }
            }

            // 4. Spending Trends
            item {
                SpendingTrendChartCard(spendingTrends)
            }

            // 5. Accounts Breakdown
            item {
                AccountBreakdownSection(accountBreakdown, currencyFormatter)
            }

            // 6. Weekly Patterns
            item {
                DayOfWeekHeatmapCard(dayOfWeekSpend)
            }

            // 7. Frequent Merchants
            item {
                MerchantAnalyticsSection(merchantAnalytics.take(5), currencyFormatter)
            }
            
            // 8. Detailed Category List
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("Categorical Spending", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
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

// Extension to capitalize first letter
fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

@Composable
fun BudgetRealityCard(status: BudgetStatus, dailyAllowance: Double?, formatter: NumberFormat, onClick: () -> Unit) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clickable { onClick() }.shadow(4.dp, RoundedCornerShape(28.dp)),
        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("MONTHLY BUDGET", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                Icon(Icons.Default.ArrowForwardIos, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(formatter.format(status.totalSpent), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                Text("of ${formatter.format(status.totalLimit)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            val progress = status.percentageUsed.coerceIn(0f, 1f)
            val barColor = when {
                progress > 0.9f -> ColorError
                progress > 0.7f -> ColorWarning
                else -> SecondaryEmerald
            }
            
            Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(Slate800)) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(barColor))
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(
                        if (status.remaining > 0) "${formatter.format(status.remaining)} left" else "Budget Exceeded",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (status.remaining > 0) SecondaryEmerald else ColorError
                    )
                    
                    if (dailyAllowance != null && status.remaining > 0) {
                        Surface(
                            color = ColorWarning.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Icon(Icons.Default.FlashOn, null, tint = ColorWarning, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "DAILY ALLOWANCE: ${formatter.format(dailyAllowance)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("used", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when(prediction.status) {
                        "SAFE" -> Icons.Default.CheckCircle
                        "WARNING" -> Icons.Default.Error
                        else -> Icons.Default.Cancel
                    },
                    null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when(prediction.status) {
                        "SAFE" -> "Spending is On Track"
                        "WARNING" -> "Approaching Monthly Limit"
                        else -> "Expected to Overspend"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                "Projected Month-end Total",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                formatter.format(prediction.predictedSpent),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun SpendingPieChartCard(data: List<Pair<String, Double>>, total: Double) {
    var selectedIndex by remember { mutableStateOf(-1) }
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    val animatedScale by animateFloatAsState(
        targetValue = if (selectedIndex != -1) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(28.dp))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val index = calculatePieIndex(offset, size, data, total)
                            selectedIndex = if (selectedIndex == index) -1 else index
                        }
                    }
                    .pointerInput(data) {
                        detectDragGestures(
                            onDragEnd = { /* keep last index */ },
                            onDragCancel = { /* keep last index */ },
                            onDrag = { change, _ ->
                                val index = calculatePieIndex(change.position, size, data, total)
                                if (index != -1) selectedIndex = index
                            }
                        )
                    }
                ) {
                    var startAngle = -90f
                    if (data.isEmpty()) {
                        drawCircle(color = Slate800, style = Stroke(width = 24.dp.toPx()))
                    } else {
                        data.forEachIndexed { index, (cat, amount) ->
                            val sweepAngle = (amount / total).toFloat() * 360f
                            val isSelected = index == selectedIndex
                            
                            val strokeWidth = if (isSelected) 40.dp.toPx() else 28.dp.toPx()
                            val color = getCategoryColor(cat)
                            
                            val finalColor = when {
                                selectedIndex == -1 -> color
                                isSelected -> color
                                else -> color.copy(alpha = 0.2f)
                            }
                            
                            drawArc(
                                color = finalColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.scale(animatedScale)
                ) {
                    if (selectedIndex != -1 && selectedIndex < data.size) {
                        val (cat, amount) = data[selectedIndex]
                        Text(
                            cat.uppercase(), 
                            style = MaterialTheme.typography.labelSmall, 
                            color = TextSecondary, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            formatter.format(amount), 
                            style = MaterialTheme.typography.headlineSmall, 
                            fontWeight = FontWeight.ExtraBold, 
                            color = TextPrimary
                        )
                        Surface(
                            color = getCategoryColor(cat).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                "${((amount/total)*100).roundToInt()}%", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = getCategoryColor(cat), 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            "${data.size}", 
                            style = MaterialTheme.typography.displaySmall, 
                            fontWeight = FontWeight.Black, 
                            color = TextPrimary
                        )
                        Text(
                            "CATEGORIES", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = TextSecondary,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center
            ) {
                data.forEachIndexed { index, (cat, _) ->
                    val isSelected = index == selectedIndex
                    Surface(
                        onClick = { selectedIndex = if (selectedIndex == index) -1 else index },
                        color = if (isSelected) getCategoryColor(cat).copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(getCategoryColor(cat), CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                cat, 
                                style = MaterialTheme.typography.labelMedium, 
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculatePieIndex(offset: Offset, size: IntSize, data: List<Pair<String, Double>>, total: Double): Int {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val x = offset.x - centerX
    val y = offset.y - centerY
    
    val distance = sqrt(x*x + y*y)
    val outerRadius = min(size.width, size.height) / 2f
    val innerRadius = outerRadius - 60.dp.value
    
    if (distance > outerRadius || distance < innerRadius * 0.4f) return -1

    var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
    if (angle < 0) angle += 360f
    
    val adjustedAngle = (angle + 90f) % 360f
    
    var currentAngle = 0f
    data.forEachIndexed { index, (_, amount) ->
        val sweepAngle = (amount / total).toFloat() * 360f
        if (adjustedAngle >= currentAngle && adjustedAngle < currentAngle + sweepAngle) {
            return index
        }
        currentAngle += sweepAngle
    }
    return -1
}

@Composable
fun SpendingTrendChartCard(trends: List<DailyTrend>) {
    var selectedTrend by remember { mutableStateOf<DailyTrend?>(null) }
    var touchX by remember { mutableStateOf(-1f) }
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Spending Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (selectedTrend != null) {
                    Text(
                        "${selectedTrend!!.dateLabel}: ${formatter.format(selectedTrend!!.amount)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimarySteelBlue
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            if (trends.isEmpty()) {
                Box(modifier = Modifier.height(180.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No data available", color = TextSecondary)
                }
            } else {
                Box(modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .pointerInput(trends) {
                        detectDragGestures(
                            onDragEnd = { selectedTrend = null; touchX = -1f },
                            onDragCancel = { selectedTrend = null; touchX = -1f },
                            onDrag = { change, _ ->
                                touchX = change.position.x
                                val width = size.width
                                val spacing = width / (trends.size.coerceAtLeast(2) - 1).coerceAtLeast(1)
                                val index = (touchX / spacing).roundToInt().coerceIn(0, trends.size - 1)
                                selectedTrend = trends[index]
                            }
                        )
                    }
                    .pointerInput(trends) {
                        detectTapGestures { offset ->
                            touchX = offset.x
                            val width = size.width
                            val spacing = width / (trends.size.coerceAtLeast(2) - 1).coerceAtLeast(1)
                            val index = (touchX / spacing).roundToInt().coerceIn(0, trends.size - 1)
                            selectedTrend = trends[index]
                        }
                    }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxAmount = trends.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
                        val width = size.width
                        val height = size.height
                        val spacing = width / (trends.size.coerceAtLeast(2) - 1).coerceAtLeast(1)
                        
                        val path = Path()
                        val fillPath = Path()
                        
                        trends.forEachIndexed { index, trend ->
                            val x = index * spacing
                            val y = height - (trend.amount / maxAmount * height).toFloat()
                            if (index == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                            if (index == trends.size - 1) {
                                fillPath.lineTo(x, height)
                                fillPath.close()
                            }
                        }
                        
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(PrimarySteelBlue.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                        
                        drawPath(
                            path = path,
                            color = PrimarySteelBlue,
                            style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )

                        if (selectedTrend != null) {
                            val index = trends.indexOf(selectedTrend!!)
                            val x = index * spacing
                            drawLine(
                                color = PrimarySteelBlue.copy(alpha = 0.5f),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                            drawCircle(
                                color = PrimarySteelBlue,
                                radius = 6.dp.toPx(),
                                center = Offset(x, height - (selectedTrend!!.amount / maxAmount * height).toFloat())
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
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
fun AccountBreakdownSection(accounts: List<AccountBreakdown>, formatter: NumberFormat) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Accounts Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        accounts.forEach { account ->
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.05f))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).background(Slate800, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, null, tint = PrimarySteelBlue, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.accountName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Spent this month", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatter.format(account.totalSpent), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        account.latestBalance?.let {
                            Text("Bal: ${formatter.format(it)}", style = MaterialTheme.typography.labelSmall, color = SecondaryEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayOfWeekHeatmapCard(daySpend: List<DayOfWeekSpend>) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Weekly Patterns", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxAmount = daySpend.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
                daySpend.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        val barHeightRatio = (day.amount / maxAmount).toFloat()
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(barHeightRatio.coerceAtLeast(0.1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (day.amount == maxAmount) PrimarySteelBlue 
                                    else PrimarySteelBlue.copy(alpha = 0.4f)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            day.dayName.take(1), 
                            style = MaterialTheme.typography.labelSmall, 
                            fontWeight = if (day.amount == maxAmount) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.amount == maxAmount) TextPrimary else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MerchantAnalyticsSection(merchants: List<MerchantAnalytics>, formatter: NumberFormat) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("Frequent Merchants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        merchants.forEach { merchant ->
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(Slate800, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(merchant.merchantName.take(1).uppercase(), color = PrimarySteelBlue, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(merchant.merchantName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${merchant.transactionCount} times • ${merchant.category}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Text(formatter.format(merchant.totalSpent), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(getCategoryColor(name).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(10.dp).background(getCategoryColor(name), CircleShape))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("$percentage% of total", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Text(formatter.format(amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
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
        else -> Color(0xFF6366F1)
    }
}
