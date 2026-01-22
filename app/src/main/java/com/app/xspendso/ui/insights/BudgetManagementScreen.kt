package com.app.xspendso.ui.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.xspendso.ui.dashboard.DashboardViewModel
import com.app.xspendso.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetManagementScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val budget by viewModel.currentBudget.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    
    val categories = remember(transactions) {
        transactions.map { it.category }.distinct().filter { it != "Income" && it != "Refund" }
    }

    var totalLimit by remember(budget) { mutableStateOf(budget?.totalLimit?.toString() ?: "0") }
    val categoryLimits = remember(budget) { 
        mutableStateMapOf<String, String>().apply {
            budget?.categoryLimits?.forEach { (cat, limit) ->
                put(cat, limit.toString())
            }
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Budget Planner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val total = totalLimit.toDoubleOrNull() ?: 0.0
                            viewModel.setMonthlyBudget(total)
                            categoryLimits.forEach { (cat, limitStr) ->
                                val limit = limitStr.toDoubleOrNull() ?: 0.0
                                viewModel.setCategoryBudget(cat, limit)
                            }
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimarySteelBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = AppSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Overall Limit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("How much do you want to spend in total this month?", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = totalLimit,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) totalLimit = it },
                            placeholder = { Text("0.00") },
                            modifier = Modifier.fillMaxWidth(),
                            prefix = { Text("₹ ", color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimarySteelBlue,
                                unfocusedBorderColor = Slate700,
                                focusedContainerColor = Slate800.copy(alpha = 0.3f),
                                unfocusedContainerColor = Slate800.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = PrimarySteelBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Category Targets (Optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Text("Set specific limits to get alerts when you overspend", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            items(categories) { category ->
                CategoryBudgetInput(
                    category = category,
                    value = categoryLimits[category] ?: "0",
                    onValueChange = { categoryLimits[category] = it }
                )
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun CategoryBudgetInput(category: String, value: String, onValueChange: (String) -> Unit) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            OutlinedTextField(
                value = if (value == "0") "" else value,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onValueChange(it) },
                modifier = Modifier.width(120.dp),
                placeholder = { Text("Optional", fontSize = 12.sp) },
                prefix = { Text("₹", style = MaterialTheme.typography.labelSmall, color = TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimarySteelBlue,
                    unfocusedBorderColor = Slate700,
                    focusedContainerColor = Slate800.copy(alpha = 0.2f),
                    unfocusedContainerColor = Slate800.copy(alpha = 0.2f)
                )
            )
        }
    }
}
