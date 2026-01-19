package com.app.xspendso.ui.people

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.app.xspendso.data.ContactLedger
import com.app.xspendso.data.LoanTransaction
import com.app.xspendso.data.LoanType
import com.app.xspendso.sms.PhoneContact
import com.app.xspendso.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    viewModel: PeopleViewModel
) {
    val context = LocalContext.current
    val contacts by viewModel.allContacts.collectAsState()
    val totalToReceive by viewModel.totalToReceive.collectAsState()
    val totalToPay by viewModel.totalToPay.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val hasContactPermission by viewModel.contactPermissionGranted.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    var showContactPicker by remember { mutableStateOf(false) }
    var selectedContactLedger by remember { mutableStateOf<ContactLedger?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var quickLogContact by remember { mutableStateOf<ContactLedger?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setContactPermissionGranted(isGranted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setContactPermissionGranted(granted)
    }

    if (showManualAddDialog) {
        ManualAddContactDialog(
            onDismiss = { showManualAddDialog = false },
            onConfirm = { name, phone ->
                viewModel.addManualContact(name, phone)
                showManualAddDialog = false
            }
        )
    }

    if (quickLogContact != null) {
        AddLoanDialog(
            onDismiss = { quickLogContact = null },
            onConfirm = { amount, type, remark, date ->
                viewModel.addTransaction(quickLogContact!!.contactId, amount, type, remark, date)
                quickLogContact = null
            }
        )
    }

    if (showContactPicker) {
        ContactPickerBottomSheet(
            viewModel = viewModel,
            onDismiss = { showContactPicker = false },
            onContactSelected = { phoneContact ->
                viewModel.addContactFromPhone(phoneContact.name, phoneContact.phone, phoneContact.photoUri)
                showContactPicker = false
            },
            onAddManually = {
                showContactPicker = false
                showManualAddDialog = true
            }
        )
    }

    if (selectedContactLedger != null) {
        ContactDetailBottomSheet(
            contactId = selectedContactLedger!!.contactId,
            viewModel = viewModel,
            onDismiss = { selectedContactLedger = null },
            currencyFormatter = currencyFormatter
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        PeopleHeader(
            totalToReceive = totalToReceive,
            totalToPay = totalToPay,
            currencyFormatter = currencyFormatter
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search", color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, null, tint = TextSecondary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimarySteelBlue,
                        unfocusedBorderColor = Slate700,
                        unfocusedContainerColor = AppSurface,
                        focusedContainerColor = AppSurface
                    ),
                    singleLine = true
                )
                
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.background(AppSurface, RoundedCornerShape(12.dp)).size(52.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, null, tint = PrimarySteelBlue)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(AppSurface)
                    ) {
                        PeopleSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.name.lowercase().replaceFirstChar { it.uppercase() }, color = TextPrimary) },
                                onClick = {
                                    viewModel.onSortOrderChange(order)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == order) Icon(Icons.Default.Check, null, tint = PrimarySteelBlue)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!hasContactPermission && contacts.isEmpty()) {
                PermissionPrompt(onGrant = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) })
            } else if (contacts.isEmpty()) {
                EmptyPeopleState(
                    isSearch = searchQuery.isNotEmpty(),
                    onAddClick = { showContactPicker = true }
                )
            } else {
                PeopleListContent(
                    contacts = contacts,
                    onContactClick = { selectedContactLedger = it },
                    onUpiClick = { contact ->
                        viewModel.initiateUpiPayment(contact)
                    },
                    onQuickAddClick = { contact ->
                        quickLogContact = contact
                    },
                    currencyFormatter = currencyFormatter
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(
            onClick = { showContactPicker = true },
            containerColor = PrimarySteelBlue,
            contentColor = Color.Black,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Person")
        }
    }
}

@Composable
fun PeopleHeader(
    totalToReceive: Double,
    totalToPay: Double,
    currencyFormatter: NumberFormat
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).statusBarsPadding()) {
        Text("People Ledger", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryCard(
                title = "Total Receivable",
                amount = currencyFormatter.format(totalToReceive),
                color = SecondaryEmerald,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Total Payable",
                amount = currencyFormatter.format(totalToPay),
                color = ColorError,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SummaryCard(title: String, amount: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(amount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun PermissionPrompt(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Contacts, null, modifier = Modifier.size(64.dp), tint = Slate700)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Contacts Access Recommended", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Access contacts to track financial settlements. You can also add people manually.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(containerColor = PrimarySteelBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Grant Access", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyPeopleState(isSearch: Boolean, onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (isSearch) Icons.Default.SearchOff else Icons.Default.PeopleOutline,
            null,
            modifier = Modifier.size(60.dp),
            tint = Slate700
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (isSearch) "No matching records" else "No ledgers found",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
        if (!isSearch) {
            TextButton(onClick = onAddClick) {
                Text("Start your first ledger", color = PrimarySteelBlue)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PeopleListContent(
    contacts: List<ContactLedger>,
    onContactClick: (ContactLedger) -> Unit,
    onUpiClick: (ContactLedger) -> Unit,
    onQuickAddClick: (ContactLedger) -> Unit,
    currencyFormatter: NumberFormat
) {
    val groupedContacts = remember(contacts) {
        contacts.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groupedContacts.forEach { (initial, contactsInGroup) ->
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppBackground)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimarySteelBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            items(contactsInGroup, key = { it.contactId }) { contact ->
                PeopleItem(
                    contact = contact,
                    onClick = { onContactClick(contact) },
                    onUpiClick = { onUpiClick(contact) },
                    onQuickAddClick = { onQuickAddClick(contact) },
                    formatter = currencyFormatter
                )
            }
        }
    }
}

@Composable
fun PeopleItem(contact: ContactLedger, onClick: () -> Unit, onUpiClick: () -> Unit, onQuickAddClick: () -> Unit, formatter: NumberFormat) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    ContactAvatar(
                        name = contact.name,
                        photoUri = contact.photoUri,
                        size = 48.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            contact.name, 
                            style = MaterialTheme.typography.bodyLarge, 
                            fontWeight = FontWeight.Bold, 
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(contact.phone, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (abs(contact.netBalance) > 0.01 && contact.netBalance < 0) {
                        Button(
                            onClick = { onUpiClick() },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorError.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ColorError)
                        ) {
                            Text("Pay", fontSize = 12.sp, color = ColorError, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    IconButton(
                        onClick = { onQuickAddClick() },
                        modifier = Modifier.size(36.dp).background(PrimarySteelBlue.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, null, tint = PrimarySteelBlue, modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                val balanceColor = when {
                    contact.netBalance > 0.01 -> SecondaryEmerald
                    contact.netBalance < -0.01 -> ColorError
                    else -> TextSecondary
                }
                
                Column {
                    Text(
                        formatter.format(abs(contact.netBalance)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                    Text(
                        if (contact.netBalance > 0.01) "Receivable" else if (contact.netBalance < -0.01) "Payable" else "Settled",
                        style = MaterialTheme.typography.labelSmall,
                        color = balanceColor.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Details", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                        null, 
                        modifier = Modifier.size(20.dp), 
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ContactAvatar(name: String, photoUri: String?, size: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = Slate800
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = (size.value / 2.5).sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerBottomSheet(
    viewModel: PeopleViewModel,
    onDismiss: () -> Unit,
    onContactSelected: (PhoneContact) -> Unit,
    onAddManually: () -> Unit
) {
    val phoneContacts by viewModel.phoneContacts.collectAsState()
    var pickerSearchQuery by remember { mutableStateOf("") }
    
    val filteredContacts = remember(phoneContacts, pickerSearchQuery) {
        if (pickerSearchQuery.isBlank()) phoneContacts
        else phoneContacts.filter { 
            it.name.contains(pickerSearchQuery, ignoreCase = true) || 
            it.phone.contains(pickerSearchQuery) 
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.8f).padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Select Contact", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                TextButton(onClick = onAddManually) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Manually")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = pickerSearchQuery,
                onValueChange = { pickerSearchQuery = it },
                placeholder = { Text("Search name or number") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                trailingIcon = {
                    if (pickerSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { pickerSearchQuery = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredContacts) { contact ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onContactSelected(contact) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContactAvatar(name = contact.name, photoUri = contact.photoUri, size = 40.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(contact.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(contact.phone, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

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
                            onClick = onEditUpi,
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
                    onClick = onAddTransaction, 
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
                    onClick = onAddEntryClick,
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

@OptIn(ExperimentalFoundationApi::class)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditUpiDialog(
    initialUpi: String, 
    phone: String, 
    onDismiss: () -> Unit, 
    onConfirm: (String) -> Unit,
    onRemove: () -> Unit
) {
    var upi by remember { mutableStateOf(initialUpi) }
    val cleanPhone = remember(phone) { phone.filter { it.isDigit() }.takeLast(10) }
    val suggestions = remember(cleanPhone) {
        listOf("okaxis", "ybl", "paytm", "upi", "okhdfcbank", "okicici")
    }
    
    val context = LocalContext.current
    
    val qrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val scanner = BarcodeScanning.getClient()
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val image = InputImage.fromBitmap(bitmap, 0)
                
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val qrContent = barcodes.firstOrNull()?.rawValue
                        if (qrContent != null && qrContent.startsWith("upi://pay")) {
                            val extractedUpi = Uri.parse(qrContent).getQueryParameter("pa")
                            if (extractedUpi != null) {
                                upi = extractedUpi
                                Toast.makeText(context, "UPI ID Extracted!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "No valid UPI QR found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to scan QR", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Set UPI ID", color = TextPrimary)
                if (initialUpi.isNotBlank()) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, "Remove UPI", tint = ColorError)
                    }
                }
            }
        },
        text = {
            Column {
                Text("Enter the UPI ID for this contact for faster settlements.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = upi,
                        onValueChange = { upi = it },
                        placeholder = { Text("example@upi") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimarySteelBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { qrLauncher.launch("image/*") },
                        modifier = Modifier.background(AppBackground, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, tint = PrimarySteelBlue)
                    }
                }
                
                if (cleanPhone.length == 10) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Smart Suggestions", style = MaterialTheme.typography.labelSmall, color = PrimarySteelBlue, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { suffix ->
                            val suggestedId = "$cleanPhone@$suffix"
                            FilterChip(
                                selected = upi == suggestedId,
                                onClick = { upi = suggestedId },
                                label = { Text(suffix, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(upi) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySteelBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNameDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Rename Ledger", color = TextPrimary) },
        text = {
            Column {
                Text("Update the display name for this financial record.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimarySteelBlue)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySteelBlue),
                shape = RoundedCornerShape(8.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Update", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddContactDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("New Ledger Entry", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimarySteelBlue)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimarySteelBlue)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onConfirm(name, phone) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySteelBlue),
                shape = RoundedCornerShape(8.dp),
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Create Ledger", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartialSettleDialog(
    maxAmount: Double, 
    type: LoanType, 
    currencyFormatter: NumberFormat,
    onDismiss: () -> Unit, 
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf(String.format("%.2f", maxAmount)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Record Settlement", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Recording a partial or full repayment for this ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || (it.all { c -> c.isDigit() || c == '.' } && it.count { c -> c == '.' } <= 1)) amount = it },
                    label = { Text("Amount to Settle (Max ${currencyFormatter.format(maxAmount)})") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimarySteelBlue)
                )
            }
        },
        confirmButton = {
            val amountVal = amount.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { if (amountVal > 0) onConfirm(amountVal) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySteelBlue),
                shape = RoundedCornerShape(8.dp),
                enabled = amountVal > 0 && amountVal <= maxAmount + 0.01
            ) {
                Text("Confirm Payment", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanDialog(onDismiss: () -> Unit, onConfirm: (Double, LoanType, String, Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(LoanType.LENT) }
    var remark by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = dateState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Add Transaction", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = type == LoanType.LENT,
                        onClick = { type = LoanType.LENT },
                        label = { Text("Lent Money") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SecondaryEmerald.copy(alpha = 0.2f),
                            selectedLabelColor = SecondaryEmerald
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = type == LoanType.LENT,
                            selectedBorderColor = SecondaryEmerald,
                            selectedBorderWidth = 2.dp
                        )
                    )
                    FilterChip(
                        selected = type == LoanType.BORROWED,
                        onClick = { type = LoanType.BORROWED },
                        label = { Text("Borrowed Money") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorError.copy(alpha = 0.2f),
                            selectedLabelColor = ColorError
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = type == LoanType.BORROWED,
                            selectedBorderColor = ColorError,
                            selectedBorderWidth = 2.dp
                        )
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || (it.all { c -> c.isDigit() || c == '.' } && it.count { c -> c == '.' } <= 1)) amount = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimarySteelBlue)
                )
                
                OutlinedTextField(
                    value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDate)),
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    shape = RoundedCornerShape(12.dp),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Slate700,
                        disabledTextColor = TextPrimary,
                        disabledLabelColor = TextSecondary,
                        disabledTrailingIconColor = TextSecondary
                    )
                )

                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("Purpose (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimarySteelBlue)
                )
            }
        },
        confirmButton = {
            val amountVal = amount.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { 
                    if (amountVal > 0) {
                        onConfirm(amountVal, type, remark, selectedDate)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySteelBlue),
                enabled = amountVal > 0,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Log Entry", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: LoanTransaction,
    onDismiss: () -> Unit,
    onConfirm: (LoanTransaction) -> Unit,
    onDelete: () -> Unit
) {
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var type by remember { mutableStateOf(transaction.type) }
    var remark by remember { mutableStateOf(transaction.remark ?: "") }
    var selectedDate by remember { mutableLongStateOf(transaction.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Entry") },
            text = { Text("Are you sure you want to delete this record?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete()
                    showDeleteConfirm = false 
                }) { Text("Delete", color = ColorError) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = dateState.selectedDateMillis ?: transaction.date
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Modify Entry", color = TextPrimary)
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, null, tint = ColorError)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = type == LoanType.LENT,
                        onClick = { type = LoanType.LENT },
                        label = { Text("Lent Money") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SecondaryEmerald.copy(alpha = 0.2f),
                            selectedLabelColor = SecondaryEmerald
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = type == LoanType.LENT,
                            selectedBorderColor = SecondaryEmerald,
                            selectedBorderWidth = 2.dp
                        )
                    )
                    FilterChip(
                        selected = type == LoanType.BORROWED,
                        onClick = { type = LoanType.BORROWED },
                        label = { Text("Borrowed Money") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorError.copy(alpha = 0.2f),
                            selectedLabelColor = ColorError
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = type == LoanType.BORROWED,
                            selectedBorderColor = ColorError,
                            selectedBorderWidth = 2.dp
                        )
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || (it.all { c -> c.isDigit() || c == '.' } && it.count { c -> c == '.' } <= 1)) amount = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimarySteelBlue)
                )

                OutlinedTextField(
                    value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedDate)),
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    shape = RoundedCornerShape(12.dp),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Slate700,
                        disabledTextColor = TextPrimary,
                        disabledLabelColor = TextSecondary,
                        disabledTrailingIconColor = TextSecondary
                    )
                )

                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("Purpose") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimarySteelBlue)
                )
            }
        },
        confirmButton = {
            val amountVal = amount.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { 
                    if (amountVal > 0) {
                        onConfirm(transaction.copy(amount = amountVal, type = type, remark = remark, date = selectedDate))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimarySteelBlue),
                enabled = amountVal > 0,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Update Log", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
