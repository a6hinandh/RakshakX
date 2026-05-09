package com.security.rakshakx.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.*
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatLogsScreen() {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var threats by remember { mutableStateOf<List<ThreatLogEntry>>(emptyList()) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            threats = try { ThreatLogRepository.getAllThreats(context) } catch (_: Exception) { emptyList() }
            isLoading = false
        }
    }

    val filteredThreats = threats.filter { entry ->
        val channelMatch = selectedChannel == null || entry.channel == selectedChannel
        val searchMatch = searchQuery.isBlank() ||
            entry.title.contains(searchQuery, true) ||
            entry.description.contains(searchQuery, true) ||
            entry.source.contains(searchQuery, true)
        channelMatch && searchMatch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Text(
            "Threat Intelligence",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${threats.size} events across all channels",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search threats...", color = colors.textMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = colors.textMuted) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.border,
                focusedContainerColor = colors.cardBackground,
                unfocusedContainerColor = colors.cardBackground,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.primary,
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Channel filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedChannel == null,
                onClick = { selectedChannel = null },
                label = { Text("All", style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary.copy(alpha = 0.15f),
                    selectedLabelColor = colors.primary,
                    containerColor = colors.cardBackground,
                    labelColor = colors.textSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = colors.border,
                    selectedBorderColor = colors.primary.copy(alpha = 0.3f),
                    enabled = true,
                    selected = selectedChannel == null
                )
            )
            Channel.entries.forEach { channel ->
                FilterChip(
                    selected = selectedChannel == channel,
                    onClick = { selectedChannel = if (selectedChannel == channel) null else channel },
                    label = { Text(channel.label, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = {
                        Icon(channel.icon, null, modifier = Modifier.size(16.dp),
                            tint = if (selectedChannel == channel) channel.color else colors.textMuted)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = channel.color.copy(alpha = 0.1f),
                        selectedLabelColor = channel.color,
                        containerColor = colors.cardBackground,
                        labelColor = colors.textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = colors.border,
                        selectedBorderColor = channel.color.copy(alpha = 0.3f),
                        enabled = true,
                        selected = selectedChannel == channel
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Threat list
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else if (filteredThreats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.VerifiedUser, null, tint = colors.safe, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Threats Found", style = MaterialTheme.typography.titleMedium, color = colors.safe)
                    Text(
                        if (searchQuery.isNotBlank()) "No results for \"$searchQuery\""
                        else "All channels are clear",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(filteredThreats, key = { it.id }) { entry ->
                    ThreatCard(entry = entry)
                }
            }
        }
    }
}
