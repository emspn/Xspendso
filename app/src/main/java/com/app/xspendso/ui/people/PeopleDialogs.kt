package com.app.xspendso.ui.people

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.xspendso.data.LoanTransaction
import com.app.xspendso.data.LoanType
import com.app.xspendso.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

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
