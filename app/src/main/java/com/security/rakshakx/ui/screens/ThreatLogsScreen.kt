package com.security.rakshakx.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.security.rakshakx.ui.anim.ShimmerPlaceholder
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.data.*
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ThreatLogsScreen() {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

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

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            PageHeader(
                title = "Threat Log",
                infoText = "Browse all detected threats across SMS, Call, Web, and Email channels with severity and timestamp details.",
                trailing = {
                    IconButton(
                        onClick = {
                            isLoading = true
                            scope.launch(Dispatchers.IO) {
                                threats = try { ThreatLogRepository.getAllThreats(context) } catch (_: Exception) { emptyList() }
                                isLoading = false
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, "Refresh", tint = colors.textSecondary)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search threats...", color = colors.textMuted) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = colors.textMuted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border.copy(alpha = 0.3f),
                    focusedContainerColor = Gunmetal,
                    unfocusedContainerColor = Gunmetal,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.primary,
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedChannel == null,
                        onClick = { haptics.tick(); selectedChannel = null },
                        label = { Text("All", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primaryMuted,
                            selectedLabelColor = colors.primary,
                            containerColor = colors.surfaceElevated,
                            labelColor = colors.textMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = colors.border.copy(alpha = 0.3f),
                            selectedBorderColor = colors.primary.copy(alpha = 0.2f),
                            enabled = true,
                            selected = selectedChannel == null
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                items(Channel.entries.toList()) { channel ->
                    FilterChip(
                        selected = selectedChannel == channel,
                        onClick = { haptics.tick(); selectedChannel = if (selectedChannel == channel) null else channel },
                        label = { Text(channel.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(channel.icon, null, modifier = Modifier.size(14.dp),
                                tint = if (selectedChannel == channel) channel.color else colors.textMuted)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = channel.color.copy(alpha = 0.1f),
                            selectedLabelColor = channel.color,
                            containerColor = colors.surfaceElevated,
                            labelColor = colors.textMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = colors.border.copy(alpha = 0.3f),
                            selectedBorderColor = channel.color.copy(alpha = 0.2f),
                            enabled = true,
                            selected = selectedChannel == channel
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    repeat(4) { ShimmerPlaceholder(height = 90.dp) }
                }
            } else if (filteredThreats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.VerifiedUser, null, tint = colors.safe.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("All Clear", style = MaterialTheme.typography.titleLarge, color = colors.safe)
                        Text(
                            if (searchQuery.isNotBlank()) "No results for \"$searchQuery\"" else "No threats detected",
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
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 6 }
                        ) {
                            ThreatCard(entry = entry)
                        }
                    }
                    item { RakshakXFooter() }
                }
            }
        }
    }
}
