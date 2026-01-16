package com.app.xspendso.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.xspendso.data.CategorizationRule
import com.app.xspendso.ui.dashboard.DashboardViewModel

@Composable
fun RuleManagementScreen(
    viewModel: DashboardViewModel
) {
    val rules by viewModel.categorizationRules.collectAsState()
    var showAddRuleDialog by remember { mutableStateOf(false) }

    if (showAddRuleDialog) {
        AddRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onConfirm = { pattern, category ->
                viewModel.addCategorizationRule(pattern, category)
                showAddRuleDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E7FF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF4338CA))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Rules automatically categorize transactions based on merchant names. Adding a rule will update your entire history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4338CA)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Active Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddRuleDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Rule")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (rules.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No custom rules yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rules) { rule ->
                    RuleItem(rule = rule, onDelete = { viewModel.deleteCategorizationRule(rule) })
                }
            }
        }
    }
}

@Composable
fun RuleItem(rule: CategorizationRule, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Merchant contains: \"${rule.merchantPattern}\"", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(text = "Category: ${rule.category}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun AddRuleDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var pattern by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    val categories = listOf("General", "Food & Dining", "Travel", "Shopping", "Bills & Utilities", "Groceries", "Entertainment", "Health")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Categorization Rule") },
        text = {
            Column {
                Text("Transactions matching this text will be auto-categorized.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Merchant Pattern (e.g. Swiggy)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Assign to Category", style = MaterialTheme.typography.labelMedium)
                // Simplified selection for dialog
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(categories) { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(selected = category == cat, onClick = { category = cat })
                            Text(cat, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { 
            Button(onClick = { onConfirm(pattern, category) }, enabled = pattern.isNotBlank()) { Text("Save Rule") } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
