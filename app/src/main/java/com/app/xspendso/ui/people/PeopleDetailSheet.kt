package com.app.xspendso.ui.people

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.xspendso.data.ContactLedger
import com.app.xspendso.data.LoanTransaction
import com.app.xspendso.data.LoanType
import com.app.xspendso.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailBottomSheet(
    contactId: Long,
    viewModel: PeopleViewModel,
    onDismiss: () -> Unit,
    currencyFormatter: NumberFormat
) {
    val context = LocalContext.current
    val contacts by viewModel.allContacts.collectAsState()
    val contact = remember(contacts, contactId) { contacts.find { it.contactId == contactId } }
    
    if (contact == null) return

    val transactions by viewModel.getTransactionsForContact(contact.contactId).collectAsState(initial = emptyList())
    var editingTransaction by remember { mutableStateOf<LoanTransaction?>(null) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var showDeleteContactConfirm by remember { mutableStateOf(false) }
    var showEditUpiDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showPartialSettleDialog by remember { mutableStateOf(false) }
    
    val selectedIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode = selectedIds.isNotEmpty()

    if (showPartialSettleDialog) {
        PartialSettleDialog(
            maxAmount = abs(contact.netBalance),
            type = if (contact.netBalance > 0) LoanType.LENT else LoanType.BORROWED,
            currencyFormatter = currencyFormatter,
            onDismiss = { showPartialSettleDialog = false },
            onConfirm = { amount ->
                viewModel.settlePartialAmount(contact.contactId, amount, if (contact.netBalance > 0) LoanType.LENT else LoanType.BORROWED)
                showPartialSettleDialog = false
                Toast.makeText(context, "Balance updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showEditUpiDialog) {
        EditUpiDialog(
            initialUpi = contact.upiId ?: "",
            phone = contact.phone,
            onDismiss = { showEditUpiDialog = false },
            onConfirm = { newUpi ->
                viewModel.updateContactUpi(contact.contactId, newUpi)
                showEditUpiDialog = false
            },
            onRemove = {
                viewModel.updateContactUpi(contact.contactId, "")
                showEditUpiDialog = false
            }
        )
    }

    if (showEditNameDialog) {
        EditNameDialog(
            initialName = contact.name,
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                viewModel.updateContactName(contact.contactId, newName)
                showEditNameDialog = false
            }
        )
    }

    if (showAddLoanDialog) {
        AddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onConfirm = { amount, type, remark, date ->
                viewModel.addTransaction(contact.contactId, amount, type, remark, date)
                showAddLoanDialog = false
            }
        )
    }

    if (editingTransaction != null) {
        EditTransactionDialog(
            transaction = editingTransaction!!,
            onDismiss = { editingTransaction = null },
            onConfirm = { updatedTx ->
                viewModel.updateTransaction(updatedTx)
                editingTransaction = null
            },
            onDelete = {
                viewModel.deleteTransaction(editingTransaction!!.id)
                editingTransaction = null
            }
        )
    }

    if (showDeleteContactConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteContactConfirm = false },
            title = { Text("Remove Ledger") },
            text = { Text("Are you sure you want to delete the records for ${contact.name}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.deleteContact(contact)
                    showDeleteContactConfirm = false
                    onDismiss()
                }) { Text("Delete", color = ColorError) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteContactConfirm = false }) { Text("Cancel") }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            DetailHeader(
                contact = contact,
                selectionCount = selectedIds.size,
                isSelectionMode = isSelectionMode,
                onCloseSelection = { selectedIds.clear() },
                onDeleteSelection = { 
                    viewModel.deleteMultipleTransactions(contact.contactId, selectedIds.toList())
                    selectedIds.clear()
                },
                onDeleteContact = { showDeleteContactConfirm = true },
                onShareReminder = { viewModel.shareReminder(contact) },
                onExport = { viewModel.exportContactLedger(contact, transactions) },
                onWhatsApp = { viewModel.shareViaWhatsApp(contact) },
                onEditUpi = { showEditUpiDialog = true },
                onEditName = { showEditNameDialog = true },
                onScanP2P = { 
                    viewModel.scanAndImportP2PTransactions(contact)
                    Toast.makeText(context, "Checking bank logs...", Toast.LENGTH_SHORT).show()
                },
                onAddTransaction = { showAddLoanDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isSelectionMode) {
                BalanceSummaryCard(
                    netBalance = contact.netBalance,
                    currencyFormatter = currencyFormatter,
                    onAddEntryClick = { showAddLoanDialog = true },
                    onSettleClick = { showPartialSettleDialog = true }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("Log History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("No transactions logged", color = TextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 400.dp)) {
                    items(transactions, key = { it.id }) { tx ->
                        val isSelected = selectedIds.contains(tx.id)
                        LoanTxRow(
                            tx = tx, 
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            formatter = currencyFormatter, 
                            onToggleSettled = { 
                                if (!isSelectionMode) viewModel.toggleSettlement(tx.id)
                                else {
                                    if (isSelected) selectedIds.remove(tx.id) else selectedIds.add(tx.id)
                                }
                            },
                            onRowClick = {
                                if (isSelectionMode) {
                                    if (isSelected) selectedIds.remove(tx.id) else selectedIds.add(tx.id)
                                } else {
                                    editingTransaction = tx
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    selectedIds.add(tx.id)
                                }
                            }
                        )
                        HorizontalDivider(color = GlassWhite, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isSelectionMode && abs(contact.netBalance) > 0.01) {
                OutlinedButton(
                    onClick = { 
                        viewModel.initiateUpiPayment(contact)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimarySteelBlue)
                ) {
                    Icon(Icons.Default.Payments, null, tint = PrimarySteelBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (contact.netBalance < 0) "Pay Total Payable" else "Request Total Receivable", color = PrimarySteelBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailHeader(
    contact: ContactLedger,
    selectionCount: Int,
    isSelectionMode: Boolean,
    onCloseSelection: () -> Unit,
    onDeleteSelection: () -> Unit,
    onDeleteContact: () -> Unit,
    onShareReminder: () -> Unit,
    onExport: () -> Unit,
    onWhatsApp: () -> Unit,
    onEditUpi: () -> Unit,
    onEditName: () -> Unit,
    onScanP2P: () -> Unit,
    onAddTransaction: () -> Unit
) {
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("Delete Transactions") },
            text = { Text("Delete $selectionCount selected records?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDeleteSelection()
                    showBulkDeleteConfirm = false 
                }) { Text("Delete", color = ColorError) }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (isSelectionMode) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCloseSelection) {
                Icon(Icons.Default.Close, null, tint = TextPrimary)
            }
            Text(
                "$selectionCount Selected", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold, 
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showBulkDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, null, tint = ColorError)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 56.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact.name, 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold, 
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(onClick = onEditName, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                            Icon(Icons.Default.Edit, "Edit Name", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                    if (contact.upiId != null && contact.upiId.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = contact.upiId, 
                                style = MaterialTheme.typography.bodySmall, 
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(onClick = onEditUpi, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                                Icon(Icons.Default.Edit, "Edit UPI", tint = TextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    } else {
                        TextButton(
                            onClick = { onEditUpi() },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = PrimarySteelBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add UPI ID", fontSize = 12.sp, color = PrimarySteelBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                IconButton(
                    onClick = { onAddTransaction() }, 
                    modifier = Modifier.size(40.dp).background(PrimarySteelBlue.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, "Add Entry", tint = PrimarySteelBlue)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onScanP2P, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Sync, "Sync", tint = PrimarySteelBlue)
                }
                IconButton(onClick = onWhatsApp, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, "WhatsApp", tint = SecondaryEmerald)
                }
                IconButton(onClick = onShareReminder, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Share, "Share", tint = PrimarySteelBlue)
                }
                IconButton(onClick = onExport, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Download, "Export", tint = TextPrimary)
                }
                IconButton(onClick = onDeleteContact, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = ColorError)
                }
            }
        }
    }
}

@Composable
fun BalanceSummaryCard(
    netBalance: Double,
    currencyFormatter: NumberFormat,
    onAddEntryClick: () -> Unit,
    onSettleClick: () -> Unit
) {
    Surface(
        color = Slate800,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Pending Balance", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(
                    currencyFormatter.format(abs(netBalance)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (netBalance > 0.01) SecondaryEmerald else if (netBalance < -0.01) ColorError else TextSecondary
                )
                Text(
                    if (netBalance > 0.01) "Receivable" else if (netBalance < -0.01) "Payable" else "Balanced",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (netBalance > 0.01) SecondaryEmerald.copy(alpha = 0.8f) else ColorError.copy(alpha = 0.8f)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (abs(netBalance) > 0.01) {
                    OutlinedButton(
                        onClick = onSettleClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
                    ) {
                        Text("Record", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = { onAddEntryClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimarySteelBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Entry", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LoanTxRow(
    tx: LoanTransaction, 
    isSelected: Boolean,
    isSelectionMode: Boolean,
    formatter: NumberFormat, 
    onToggleSettled: () -> Unit, 
    onRowClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tx.date))
    val isLent = tx.type == LoanType.LENT
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onRowClick,
                onLongClick = onLongClick
            )
            .background(if (isSelected) PrimarySteelBlue.copy(alpha = 0.1f) else Color.Transparent)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onRowClick() },
                colors = CheckboxDefaults.colors(checkedColor = PrimarySteelBlue)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        val textAlpha = if (tx.isSettled) 0.5f else 1f
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isLent) "Lent Money" else "Borrowed Money", 
                style = MaterialTheme.typography.bodyMedium, 
                color = if (isLent) SecondaryEmerald else ColorError, 
                fontWeight = FontWeight.Bold,
                textDecoration = if (tx.isSettled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )
            Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = textAlpha))
            if (!tx.remark.isNullOrBlank()) {
                Text(tx.remark, style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = textAlpha), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }
        
        Text(
            formatter.format(tx.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = (if (isLent) SecondaryEmerald else ColorError).copy(alpha = if (tx.isSettled) 0.5f else 1f),
            textDecoration = if (tx.isSettled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
        )
        
        Spacer(modifier = Modifier.width(16.dp))

        if (!isSelectionMode) {
            if (!tx.isSettled) {
                Button(
                    onClick = onToggleSettled,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isLent) SecondaryEmerald.copy(0.15f) else ColorError.copy(0.15f)),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isLent) SecondaryEmerald else ColorError)
                ) {
                    Text("Settled", fontSize = 11.sp, color = if (isLent) SecondaryEmerald else ColorError, fontWeight = FontWeight.Bold)
                }
            } else {
                IconButton(onClick = onToggleSettled, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = SecondaryEmerald, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
