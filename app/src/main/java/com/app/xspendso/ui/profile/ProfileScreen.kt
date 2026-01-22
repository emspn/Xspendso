package com.app.xspendso.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.xspendso.data.PrefsManager
import com.app.xspendso.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    prefsManager: PrefsManager,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteData: () -> Unit,
    onNavigateToRules: () -> Unit,
    onForceReparse: () -> Unit
) {
    var name by remember { mutableStateOf(prefsManager.userName ?: "User") }
    var isEditingName by remember { mutableStateOf(false) }
    val email = prefsManager.userEmail ?: "Not provided"
    val photoUrl = prefsManager.userPhotoUrl

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReparseDialog by remember { mutableStateOf(false) }
    var showUpiDialog by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(prefsManager.isBiometricEnabled) }
    var syncFrequency by remember { mutableFloatStateOf(prefsManager.syncFrequencyHours.toFloat()) }
    var userUpi by remember { mutableStateOf(prefsManager.userUpiId ?: "") }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = AppSurface,
            title = { Text("Delete All Data", color = TextPrimary) },
            text = { Text("This will permanently remove all your local transaction records. This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorError)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showReparseDialog) {
        AlertDialog(
            onDismissRequest = { showReparseDialog = false },
            containerColor = AppSurface,
            title = { Text("Fix History & Reparse", color = TextPrimary) },
            text = { Text("This will clear your current ledger and re-scan all SMS messages using the improved parser.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onForceReparse()
                        showReparseDialog = false
                    }
                ) {
                    Text("Start Repair", color = PrimarySteelBlue)
                }
            },
            dismissButton = { TextButton(onClick = { showReparseDialog = false }) { Text("Cancel") } }
        )
    }

    if (showUpiDialog) {
        AlertDialog(
            onDismissRequest = { showUpiDialog = false },
            containerColor = AppSurface,
            title = { Text("My Payment Details", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("These details will be used to generate repayment links for others to pay you.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    OutlinedTextField(
                        value = userUpi,
                        onValueChange = { userUpi = it },
                        label = { Text("My UPI ID") },
                        placeholder = { Text("username@upi") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimarySteelBlue)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefsManager.userUpiId = userUpi
                        showUpiDialog = false
                    }
                ) {
                    Text("Save Details", color = PrimarySteelBlue)
                }
            },
            dismissButton = { TextButton(onClick = { showUpiDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Profile Picture Section
            Box(contentAlignment = Alignment.BottomEnd) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, PrimarySteelBlue, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = AppSurface,
                        border = androidx.compose.foundation.BorderStroke(2.dp, PrimarySteelBlue)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = name.take(1).uppercase(),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimarySteelBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 1. Identity Card
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassWhite.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Name row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NAME", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                            if (isEditingName) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimarySteelBlue)
                                )
                            } else {
                                Text(name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        IconButton(onClick = { 
                            if (isEditingName) prefsManager.userName = name
                            isEditingName = !isEditingName 
                        }) {
                            Icon(if (isEditingName) Icons.Default.Check else Icons.Default.Edit, null, tint = PrimarySteelBlue)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Email row
                    Column {
                        Text("EMAIL", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text(email, style = MaterialTheme.typography.bodyMedium, color = TextPrimary.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Settings Sections
            ProfileSettingsSection(title = "Security") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric Lock", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Text("Require fingerprint to open app", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = {
                            biometricEnabled = it
                            prefsManager.isBiometricEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimarySteelBlue)
                    )
                }
            }

            ProfileSettingsSection(title = "Payments") {
                Surface(
                    onClick = { showUpiDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("My UPI Profile", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(if (userUpi.isBlank()) "Not set" else userUpi, style = MaterialTheme.typography.bodySmall, color = if (userUpi.isBlank()) ColorError else TextSecondary)
                        }
                        Icon(Icons.Default.QrCode, null, tint = PrimarySteelBlue)
                    }
                }
            }

            ProfileSettingsSection(title = "Preference") {
                Surface(onClick = onNavigateToRules, modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Categorization Rules", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column {
                    Text("Sync Frequency: ${syncFrequency.toInt()} hours", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Slider(
                        value = syncFrequency,
                        onValueChange = { syncFrequency = it },
                        onValueChangeFinished = { prefsManager.syncFrequencyHours = syncFrequency.toInt() },
                        valueRange = 1f..24f,
                        steps = 23,
                        colors = SliderDefaults.colors(thumbColor = PrimarySteelBlue, activeTrackColor = PrimarySteelBlue)
                    )
                }
            }

            ProfileSettingsSection(title = "Maintenance") {
                Surface(onClick = { showReparseDialog = true }, modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Fix Data Errors", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text("Rescan all SMS logs", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Icon(Icons.Default.Build, null, tint = PrimarySteelBlue)
                    }
                }
            }

            // Privacy Guarantee Card
            Card(
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, null, tint = PrimarySteelBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Your financial data is processed 100% locally and never uploaded to our servers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GlassWhite.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Logout from Account", color = TextPrimary, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorError.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ColorError.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.DeleteForever, null, tint = ColorError)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Delete All Local Data", color = ColorError, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Xpendso v1.2.5 • Engine local",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
fun ProfileSettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = PrimarySteelBlue,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        content()
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = GlassWhite.copy(alpha = 0.05f))
    }
}
