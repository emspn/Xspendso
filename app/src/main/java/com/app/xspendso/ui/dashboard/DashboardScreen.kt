package com.app.xspendso.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.xspendso.data.TransactionEntity
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    var activeTab by remember { mutableStateOf("overview") }
    val transactions by viewModel.transactions.collectAsState()
    val monthlyAnalytics by viewModel.monthlyAnalytics.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Xpendso",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your unified payment tracker",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Balance Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Spent This Month", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                        }
                        Text(
                            text = currencyFormatter.format(totalSpent),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                            Text(text = " Unified Ledger Active", color = Color.Green, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Tabs
            TabRow(
                selectedTabIndex = if (activeTab == "overview") 0 else if (activeTab == "transactions") 1 else 2,
                containerColor = Color.White,
                contentColor = Color(0xFF6366F1),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (activeTab == "overview") 0 else if (activeTab == "transactions") 1 else 2]),
                        color = Color(0xFF6366F1)
                    )
                }
            ) {
                Tab(selected = activeTab == "overview", onClick = { activeTab = "overview" }) {
                    Text(text = "Overview", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = activeTab == "transactions", onClick = { activeTab = "transactions" }) {
                    Text(text = "Transactions", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = activeTab == "analytics", onClick = { activeTab = "analytics" }) {
                    Text(text = "Analytics", modifier = Modifier.padding(16.dp))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (activeTab == "overview") {
                    item {
                        CategoryBreakdown(monthlyAnalytics, totalSpent)
                    }
                    item {
                        RecentTransactionsHeader(onNavigateToTransactions)
                    }
                    items(transactions.take(5)) { transaction ->
                        TransactionItem(transaction)
                    }
                    item {
                        QuickActions(onSync = { viewModel.importTransactions() })
                    }
                } else if (activeTab == "transactions") {
                    items(transactions) { transaction ->
                        TransactionItem(transaction)
                    }
                } else {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Analytics Visualizations coming soon...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdown(analytics: Map<String, Double>, total: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Category Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (analytics.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data for this month", color = Color.Gray)
                }
            } else {
                analytics.forEach { (category, amount) ->
                    val percentage = if (total > 0) (amount / total * 100).toFloat() else 0f
                    val (color, icon) = getCategoryMeta(category)
                    CategoryItem(category, "₹${String.format("%.2f", amount)}", percentage, color, icon)
                }
            }
        }
    }
}

fun getCategoryMeta(category: String): Pair<Color, ImageVector> {
    return when (category) {
        "Food & Dining" -> Color(0xFFFF6B6B) to Icons.Default.Coffee
        "Travel" -> Color(0xFF4ECDC4) to Icons.Default.DirectionsCar
        "Shopping" -> Color(0xFFFFA07A) to Icons.Default.ShoppingBag
        "Bills & Utilities" -> Color(0xFF45B7D1) to Icons.Default.Bolt
        else -> Color(0xFF95A5A6) to Icons.Default.MoreHoriz
    }
}

@Composable
fun CategoryItem(name: String, amount: String, percentage: Float, color: Color, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = amount, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = "${String.format("%.1f", percentage)}%", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun RecentTransactionsHeader(onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Recent Transactions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        TextButton(onClick = onViewAll) {
            Text(text = "View All", color = Color(0xFF6366F1))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF6366F1))
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (transaction.amount > 0) Color(0xFFDCFCE7) else Color(0xFFF1F5F9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = if (transaction.amount > 0) Color(0xFF166534) else Color(0xFF475569)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1.0f)) {
                Text(text = transaction.merchant, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "${transaction.category} • ${transaction.method}", fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (transaction.amount > 0) "+" else ""}₹${String.format("%.2f", Math.abs(transaction.amount))}",
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.amount > 0) Color(0xFF166534) else Color.Black
                )
                val date = remember(transaction.timestamp) { 
                    val cal = Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
                    "${cal.get(Calendar.DAY_OF_MONTH)} ${cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())}"
                }
                Text(text = date, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun QuickActions(onSync: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = { },
            modifier = Modifier
                .weight(1f)
                .height(80.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Export", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Text(text = "Download PDF", fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.FileDownload, contentDescription = null)
            }
        }
        OutlinedButton(
            onClick = onSync,
            modifier = Modifier
                .weight(1f)
                .height(80.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE2E8F0))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Sync Messages", fontSize = 12.sp, color = Color.Gray)
                    Text(text = "Refresh Data", fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                }
                Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF6366F1))
            }
        }
    }
}
