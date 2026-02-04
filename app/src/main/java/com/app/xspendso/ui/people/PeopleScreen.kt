package com.app.xspendso.ui.people

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.app.xspendso.data.ContactLedger
import com.app.xspendso.sms.PhoneContact
import com.app.xspendso.ui.theme.*
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    viewModel: PeopleViewModel,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val contacts by viewModel.allContacts.collectAsState()
    val totalToReceive by viewModel.totalToReceive.collectAsState()
    val totalToPay by viewModel.totalToPay.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
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
            syncStatus = syncStatus,
            currencyFormatter = currencyFormatter,
            onProfileClick = onProfileClick,
            onSyncClick = { viewModel.syncWithCloud() }
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
