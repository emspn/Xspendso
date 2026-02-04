package com.app.xspendso.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.xspendso.data.ContactLedger
import com.app.xspendso.domain.SyncStatus
import com.app.xspendso.ui.theme.*
import java.text.NumberFormat
import kotlin.math.abs

@Composable
fun PeopleHeader(
    totalToReceive: Double,
    totalToPay: Double,
    syncStatus: SyncStatus,
    currencyFormatter: NumberFormat,
    onProfileClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "People Ledger",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                SyncStatusIndicator(syncStatus, onSyncClick)
            }
            
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(AppSurface, CircleShape)
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
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
fun SyncStatusIndicator(status: SyncStatus, onSyncClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { if (status != SyncStatus.SYNCING) onSyncClick() }
    ) {
        val (text, icon, color) = when (status) {
            SyncStatus.IDLE -> Triple("Cloud Sync", Icons.Default.CloudQueue, TextSecondary)
            SyncStatus.SYNCING -> Triple("Syncing...", Icons.Default.Sync, PrimarySteelBlue)
            SyncStatus.SUCCESS -> Triple("Synced", Icons.Default.CloudDone, SecondaryEmerald)
            SyncStatus.ERROR -> Triple("Sync Error", Icons.Default.CloudOff, ColorError)
        }
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
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
fun PeopleItem(
    contact: ContactLedger,
    onClick: () -> Unit,
    onUpiClick: () -> Unit,
    onQuickAddClick: () -> Unit,
    formatter: NumberFormat
) {
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
                    if (abs(contact.netBalance) > 0.01) {
                        if (contact.netBalance < 0) {
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
                        } else {
                            Button(
                                onClick = { onUpiClick() },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryEmerald)
                            ) {
                                Text("Request", fontSize = 12.sp, color = SecondaryEmerald, fontWeight = FontWeight.Bold)
                            }
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
