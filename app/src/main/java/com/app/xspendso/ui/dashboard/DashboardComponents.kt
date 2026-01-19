package com.app.xspendso.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.xspendso.data.ContactLedger
import com.app.xspendso.data.TransactionEntity
import com.app.xspendso.ui.people.PeopleViewModel
import com.app.xspendso.ui.theme.ColorError
import com.app.xspendso.ui.theme.SecondaryEmerald
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

val DEFAULT_CATEGORIES = listOf(
    "Food & Dining", "Shopping", "Bills & Utilities", "Groceries",
    "Travel", "Cab & Rental", "Entertainment", "Health",
    "Investment", "Education", "Gifts & Donations", "Personal Care",
    "General", "Others"
)

@Composable
fun TimeFilterDropdown(
    selectedFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.wrapContentSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = when(selectedFilter) {
                        TimeFilter.TODAY -> "Today"
                        TimeFilter.THIS_WEEK -> "Weekly"
                        TimeFilter.THIS_MONTH -> "Monthly"
                        TimeFilter.THIS_YEAR -> "This Year"
                        TimeFilter.CUSTOM -> "Custom"
                        TimeFilter.ALL_TIME -> "All Time"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
        ) {
            TimeFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = when(filter) {
                                TimeFilter.TODAY -> "Today"
                                TimeFilter.THIS_WEEK -> "Weekly"
                                TimeFilter.THIS_MONTH -> "Monthly"
                                TimeFilter.THIS_YEAR -> "This Year"
                                TimeFilter.CUSTOM -> "Custom Range"
                                TimeFilter.ALL_TIME -> "All Time"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    onClick = {
                        onFilterSelected(filter)
                        expanded = false
                    },
                    leadingIcon = {
                        val icon = when(filter) {
                            TimeFilter.TODAY -> Icons.Default.Today
                            TimeFilter.THIS_WEEK -> Icons.Default.DateRange
                            TimeFilter.THIS_MONTH -> Icons.Default.CalendarMonth
                            TimeFilter.THIS_YEAR -> Icons.Default.EventNote
                            TimeFilter.CUSTOM -> Icons.Default.DateRange
                            TimeFilter.ALL_TIME -> Icons.Default.AllInclusive
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selectedFilter == filter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryBreakdownSection(analytics: Map<String, Double>, total: Double) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Spending by Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            if (analytics.isEmpty()) {
                Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } else {
                analytics.forEach { (category, amount) ->
                    val percentage = if (total > 0) (amount / total * 100).toFloat() else 0f
                    val meta = getCategoryMeta(category)
                    CategoryItem(category, "₹${String.format(Locale.getDefault(), "%.2f", amount)}", percentage, meta.first, meta.second)
                }
            }
        }
    }
}

@Composable
fun CategoryItem(name: String, amount: String, percentage: Float, color: Color, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = amount, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "${String.format(Locale.getDefault(), "%.1f", percentage)}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun getCategoryMeta(category: String): Pair<Color, ImageVector> {
    return when (category) {
        "Food & Dining" -> Color(0xFFFF6B6B) to Icons.Default.Restaurant
        "Travel" -> Color(0xFF4ECDC4) to Icons.Default.Flight
        "Cab & Rental" -> Color(0xFF4ECDC4) to Icons.Default.DirectionsCar
        "Shopping" -> Color(0xFFFFA07A) to Icons.Default.ShoppingBag
        "Bills & Utilities" -> Color(0xFF45B7D1) to Icons.Default.Bolt
        "Groceries" -> Color(0xFF6366F1) to Icons.Default.LocalGroceryStore
        "Entertainment" -> Color(0xFF8B5CF6) to Icons.Default.PlayCircle
        "Health" -> Color(0xFFEC4899) to Icons.Default.MedicalServices
        "Investment" -> Color(0xFF10B981) to Icons.Default.TrendingUp
        "Education" -> Color(0xFFF59E0B) to Icons.Default.School
        else -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.MoreHoriz
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(transaction: TransactionEntity, onLongClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { }, onLongClick = onLongClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = transaction.counterparty, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                    if (transaction.isRecurring) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Autorenew, contentDescription = "Recurring", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                }
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
                Text(text = time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = transaction.accountSource, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = transaction.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "₹${String.format(Locale.getDefault(), "%.2f", abs(transaction.amount))}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.amount > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                )
                if (!transaction.remark.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(text = transaction.remark, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), maxLines = 1)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualPaymentSheet(onDismiss: () -> Unit, onSave: (String, Double, String, String) -> Unit) {
    var counterparty by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var type by remember { mutableStateOf("DEBIT") }
    var customCategory by remember { mutableStateOf("") }
    var showCustomCategoryInput by remember { mutableStateOf(false) }

    val allCategories = remember { (DEFAULT_CATEGORIES + "Custom +").distinct() }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
            Text("Add Transaction", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = counterparty, onValueChange = { counterparty = it }, label = { Text("Counterparty") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = amount, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = type == "DEBIT", onClick = { type = "DEBIT" }, label = { Text("Expense") }, modifier = Modifier.weight(1f))
                FilterChip(selected = type == "CREDIT", onClick = { type = "CREDIT" }, label = { Text("Income") }, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                allCategories.forEach { cat -> 
                    FilterChip(
                        selected = category == cat || (cat == "Custom +" && showCustomCategoryInput), 
                        onClick = { 
                            if (cat == "Custom +") {
                                showCustomCategoryInput = true
                            } else {
                                category = cat
                                showCustomCategoryInput = false
                            }
                        }, 
                        label = { Text(cat) }
                    ) 
                }
            }
            if (showCustomCategoryInput) {
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    label = { Text("Custom Category") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { 
                    val finalCategory = if (showCustomCategoryInput && customCategory.isNotBlank()) customCategory else category
                    onSave(counterparty, amount.toDoubleOrNull() ?: 0.0, finalCategory, type) 
                }, 
                modifier = Modifier.fillMaxWidth().height(56.dp), 
                enabled = counterparty.isNotBlank() && amount.isNotBlank(),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Save Transaction", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditSheet(
    transaction: TransactionEntity,
    peopleViewModel: PeopleViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onDelete: () -> Unit,
    onSplit: (ContactLedger) -> Unit
) {
    var counterparty by remember { mutableStateOf(transaction.counterparty) }
    var remark by remember { mutableStateOf(transaction.remark ?: "") }
    var category by remember { mutableStateOf(transaction.category) }
    var accountSource by remember { mutableStateOf(transaction.accountSource) }
    var customCategory by remember { mutableStateOf("") }
    var showCustomCategoryInput by remember { mutableStateOf(false) }
    var showSplitPicker by remember { mutableStateOf(false) }

    val allCategories = remember { (DEFAULT_CATEGORIES + "Custom +").distinct() }
    val contacts by peopleViewModel.allContacts.collectAsState()

    if (showSplitPicker) {
        ModalBottomSheet(onDismissRequest = { showSplitPicker = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 32.dp)) {
                Text("Select person to split with", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(contacts) { contact ->
                        Surface(
                            onClick = { 
                                onSplit(contact)
                                showSplitPicker = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(contact.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Edit Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Row {
                    IconButton(onClick = { showSplitPicker = true }) {
                        Icon(Icons.Default.CallSplit, contentDescription = "Split", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(value = counterparty, onValueChange = { counterparty = it }, label = { Text("Counterparty") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = accountSource, onValueChange = { accountSource = it }, label = { Text("Source") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                allCategories.forEach { cat ->
                    FilterChip(
                        selected = category == cat || (cat == "Custom +" && showCustomCategoryInput),
                        onClick = {
                            if (cat == "Custom +") {
                                showCustomCategoryInput = true
                            } else {
                                category = cat
                                showCustomCategoryInput = false
                            }
                        },
                        label = { Text(cat) }
                    )
                }
            }
            if (showCustomCategoryInput) {
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    label = { Text("Custom Category") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val finalCategory = if (showCustomCategoryInput && customCategory.isNotBlank()) customCategory else category
                    onSave(counterparty, remark, finalCategory, accountSource)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Update Transaction", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
fun QuickActions(
    isSyncing: Boolean,
    onSync: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            onClick = onExportPdf,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(16.dp),
            color = ColorError.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ColorError.copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp), tint = ColorError)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export PDF", style = MaterialTheme.typography.labelLarge, color = ColorError, fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            onClick = onExportCsv,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(16.dp),
            color = SecondaryEmerald.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryEmerald.copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp), tint = SecondaryEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export CSV", style = MaterialTheme.typography.labelLarge, color = SecondaryEmerald, fontWeight = FontWeight.Bold)
            }
        }

        FilledIconButton(
            onClick = onSync,
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                Icons.Default.Sync,
                contentDescription = "Sync",
                modifier = Modifier
                    .size(22.dp)
                    .rotate(if (isSyncing) rotation else 0f)
            )
        }
    }
}

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Text(text = "No records found", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}
