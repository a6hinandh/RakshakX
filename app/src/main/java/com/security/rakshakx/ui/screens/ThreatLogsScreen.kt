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
import androidx.compose.foundation.shape.RoundedCornerShape
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.*
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Text(
                "Verified Threats",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Text(
                "${threats.size} events across all channels",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
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
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 20.dp) // Extra padding for the last item
        ) {
            item {
                FilterChip(
                    selected = selectedChannel == null,
                    onClick = { selectedChannel = null },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White.copy(alpha = 0.1f),
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.02f),
                        labelColor = Color.White.copy(alpha = 0.5f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.White.copy(alpha = 0.05f),
                        selectedBorderColor = Color.White.copy(alpha = 0.2f),
                        enabled = true,
                        selected = selectedChannel == null
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            items(Channel.entries.toList()) { channel ->
                val accentColor = when (channel) {
                    Channel.SMS -> Color(0xFF4776E6)
                    Channel.CALL -> Color(0xFF8E54E9)
                    Channel.WEB -> Color(0xFF10B981)
                    Channel.EMAIL -> Color(0xFFEF4444)
                }
                FilterChip(
                    selected = selectedChannel == channel,
                    onClick = { selectedChannel = if (selectedChannel == channel) null else channel },
                    label = { Text(channel.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(channel.icon, null, modifier = Modifier.size(16.dp),
                            tint = if (selectedChannel == channel) accentColor else Color.White.copy(alpha = 0.3f))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.15f),
                        selectedLabelColor = accentColor,
                        containerColor = Color.White.copy(alpha = 0.02f),
                        labelColor = Color.White.copy(alpha = 0.5f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.White.copy(alpha = 0.05f),
                        selectedBorderColor = accentColor.copy(alpha = 0.3f),
                        enabled = true,
                        selected = selectedChannel == channel
                    ),
                    shape = RoundedCornerShape(12.dp)
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
                item {
                    RakshakXFooter()
                }
            }
        }
        }
    }
}
