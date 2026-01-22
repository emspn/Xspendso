package com.app.xspendso.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.xspendso.ui.theme.AppSurface
import com.app.xspendso.ui.theme.AppBackground
import com.app.xspendso.ui.theme.TextPrimary
import com.app.xspendso.ui.theme.TextSecondary
import com.app.xspendso.ui.theme.SecondaryEmerald
import java.text.NumberFormat

@Composable
fun DashboardHeader(
    searchQuery: String,
    totalSpent: Double,
    totalReceived: Double,
    isSyncing: Boolean,
    timeFilter: TimeFilter,
    currencyFormatter: NumberFormat,
    onSearchChange: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(AppBackground)) {
        DashboardSummarySection(
            totalSpent = totalSpent,
            totalReceived = totalReceived,
            isSyncing = isSyncing,
            timeFilter = timeFilter,
            currencyFormatter = currencyFormatter,
            onProfileClick = onProfileClick
        )
        DashboardSearchSection(
            searchQuery = searchQuery,
            onSearchChange = onSearchChange
        )
    }
}

@Composable
fun DashboardSummarySection(
    totalSpent: Double,
    totalReceived: Double,
    isSyncing: Boolean,
    timeFilter: TimeFilter,
    currencyFormatter: NumberFormat,
    onProfileClick: () -> Unit
) {
    val periodLabel = when(timeFilter) {
        TimeFilter.TODAY -> "Today"
        TimeFilter.THIS_WEEK -> "Weekly"
        TimeFilter.THIS_MONTH -> "Monthly"
        TimeFilter.THIS_YEAR -> "This Year"
        TimeFilter.CUSTOM -> "Custom Range"
        TimeFilter.ALL_TIME -> "All Time"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Xpendso",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Personal Finance Ledger",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
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

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = AppSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BALANCE SUMMARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = periodLabel,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Spent", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(
                            text = currencyFormatter.format(totalSpent),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Received", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(
                            text = currencyFormatter.format(totalReceived),
                            style = MaterialTheme.typography.titleLarge,
                            color = SecondaryEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isSyncing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = AppSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardSearchSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    "Search transactions...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary.copy(alpha = 0.6f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppSurface,
                unfocusedContainerColor = AppSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true
        )
    }
}
