package com.security.rakshakx.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.KeyguardManager
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.security.rakshakx.core.vault.SecureVault
import com.security.rakshakx.core.vault.VaultCategory
import com.security.rakshakx.core.vault.VaultEntry
import com.security.rakshakx.ui.anim.StaggeredEntry
import com.security.rakshakx.ui.anim.rememberHaptics
import com.security.rakshakx.ui.components.*
import com.security.rakshakx.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultScreen(onBack: () -> Unit) {
    val colors = LocalRakshakXColors.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var entries by remember { mutableStateOf<List<VaultEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<VaultEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<VaultEntry?>(null) }
    var entryToEdit by remember { mutableStateOf<VaultEntry?>(null) }

    val keyguardManager = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingAction?.invoke()
        } else {
            Toast.makeText(context, "Authentication required", Toast.LENGTH_SHORT).show()
        }
        pendingAction = null
    }

    fun authenticateAndRun(action: () -> Unit) {
        if (keyguardManager.isDeviceSecure) {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent("Secure Vault", "Please authenticate to access this secret.")
            if (intent != null) {
                pendingAction = action
                authLauncher.launch(intent)
            } else action()
        } else action()
    }

    fun refreshEntries() {
        scope.launch(Dispatchers.IO) {
            val loaded = SecureVault.getAllEntries(context)
            withContext(Dispatchers.Main) {
                entries = loaded
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { refreshEntries() }

    if (showAddDialog) {
        AddEntryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { entry ->
                scope.launch(Dispatchers.IO) {
                    SecureVault.saveEntry(context, entry)
                    withContext(Dispatchers.Main) {
                        showAddDialog = false
                        haptics.success()
                        refreshEntries()
                    }
                }
            }
        )
    }

    selectedEntry?.let { entry ->
        ViewEntryDialog(
            entry = entry,
            onDismiss = { selectedEntry = null },
            onEdit = { authenticateAndRun { entryToEdit = entry; selectedEntry = null } },
            onAuthenticate = { action -> authenticateAndRun(action) }
        )
    }

    entryToEdit?.let { entry ->
        AddEntryDialog(
            initialEntry = entry,
            onDismiss = { entryToEdit = null },
            onSave = { updatedEntry ->
                scope.launch(Dispatchers.IO) {
                    SecureVault.saveEntry(context, updatedEntry)
                    withContext(Dispatchers.Main) {
                        entryToEdit = null
                        haptics.success()
                        refreshEntries()
                    }
                }
            }
        )
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        SecureVault.deleteEntry(context, entry.id)
                        withContext(Dispatchers.Main) {
                            entryToDelete = null
                            haptics.warning()
                            refreshEntries()
                        }
                    }
                }) { Text("Delete", color = colors.critical) }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            title = { Text("Delete Entry?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${entry.title}\" will be permanently deleted from the vault.",
                    color = colors.textSecondary
                )
            },
            containerColor = colors.surfaceElevated,
            shape = RoundedCornerShape(20.dp)
        )
    }

    PremiumBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    PageHeader(
                        title = "Secure Vault",
                        infoText = "Store sensitive credentials in AES-256 encrypted local storage that never syncs to the cloud.",
                        onBack = { haptics.click(); onBack() },
                        trailing = {
                            FloatingActionButton(
                                onClick = { haptics.click(); showAddDialog = true },
                                modifier = Modifier.size(44.dp),
                                containerColor = colors.primary,
                                shape = CircleShape,
                                elevation = FloatingActionButtonDefaults.elevation(0.dp)
                            ) {
                                Icon(Icons.Filled.Add, "Add entry", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    )
                }

                item {
                    StaggeredEntry(index = 0) {
                        GlassCard(borderColor = colors.primary.copy(alpha = 0.2f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(colors.primaryMuted, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Security, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${entries.size} item${if (entries.size != 1) "s" else ""} stored",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "AndroidKeyStore · AES/GCM/256-bit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                                Icon(Icons.Filled.Lock, null, tint = colors.safe, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                if (isLoading) {
                    items(3) {
                        GlassSurface {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(colors.border.copy(alpha = 0.3f)))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(colors.border.copy(alpha = 0.3f)))
                                    Box(modifier = Modifier.fillMaxWidth(0.7f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(colors.border.copy(alpha = 0.2f)))
                                }
                            }
                        }
                    }
                } else if (entries.isEmpty()) {
                    item {
                        StaggeredEntry(index = 1) {
                            GlassCard {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Text(
                                        "Your vault is empty",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Tap + to add your first entry",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(entries) { index, entry ->
                        StaggeredEntry(index = 1 + index) {
                            GlassSurface(
                                onClick = { selectedEntry = entry; haptics.click() },
                                modifier = Modifier.combinedClickable(
                                    onClick = { selectedEntry = entry; haptics.click() },
                                    onLongClick = { entryToDelete = entry; haptics.heavyClick() }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(
                                                vaultCategoryColor(entry.category).copy(alpha = 0.15f),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            vaultCategoryIcon(entry.category),
                                            null,
                                            tint = vaultCategoryColor(entry.category),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            entry.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "••••••••",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textMuted,
                                                letterSpacing = 2.sp
                                            )
                                            IconButton(onClick = {
                                                authenticateAndRun {
                                                    entryToDelete = entry
                                                    haptics.warning()
                                                }
                                            }) {
                                                Icon(Icons.Filled.Delete, "Delete", tint = colors.critical)
                                            }
                                            Text(
                                                "·",
                                                color = colors.textMuted,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                formatDate(entry.updatedAt),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textMuted
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                vaultCategoryColor(entry.category).copy(alpha = 0.1f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            entry.category.name.take(4),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = vaultCategoryColor(entry.category),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { RakshakXFooter() }
            }
        }
    }
}

@Composable
private fun AddEntryDialog(
    initialEntry: VaultEntry? = null,
    onDismiss: () -> Unit,
    onSave: (VaultEntry) -> Unit
) {
    val colors = LocalRakshakXColors.current
    var title by remember { mutableStateOf(initialEntry?.title ?: "") }
    var content by remember { mutableStateOf(initialEntry?.content ?: "") }
    var selectedCategory by remember { mutableStateOf(initialEntry?.category ?: VaultCategory.PASSWORD) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }
    var contentError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Add Vault Entry",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title", color = colors.textMuted) },
                    leadingIcon = { Icon(Icons.Filled.Label, null, tint = colors.primary) },
                    isError = titleError,
                    supportingText = { if (titleError) Text("Title is required", color = colors.critical) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Box {
                    OutlinedTextField(
                        value = selectedCategory.name.replace("_", " "),
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Category", color = colors.textMuted) },
                        leadingIcon = {
                            Icon(vaultCategoryIcon(selectedCategory), null, tint = vaultCategoryColor(selectedCategory))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showCategoryMenu = true }) {
                                Icon(Icons.Filled.ArrowDropDown, null, tint = colors.textMuted)
                            }
                        },
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false },
                        modifier = Modifier.background(colors.surfaceElevated)
                    ) {
                        VaultCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(vaultCategoryIcon(cat), null, tint = vaultCategoryColor(cat), modifier = Modifier.size(18.dp))
                                        Text(cat.name.replace("_", " "), color = colors.textPrimary)
                                    }
                                },
                                onClick = { selectedCategory = cat; showCategoryMenu = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it; contentError = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    label = { Text("Content", color = colors.textMuted) },
                    leadingIcon = {
                        Icon(Icons.Filled.Notes, null, tint = colors.primary, modifier = Modifier.padding(top = 8.dp))
                    },
                    isError = contentError,
                    supportingText = { if (contentError) Text("Content is required", color = colors.critical) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 8
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary)
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            titleError = title.isBlank()
                            contentError = content.isBlank()
                            if (!titleError && !contentError) {
                                val entryToSave = initialEntry?.copy(
                                    title = title.trim(),
                                    content = content.trim(),
                                    category = selectedCategory,
                                    updatedAt = System.currentTimeMillis()
                                ) ?: VaultEntry(
                                    title = title.trim(),
                                    content = content.trim(),
                                    category = selectedCategory
                                )
                                onSave(entryToSave)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewEntryDialog(
    entry: VaultEntry, 
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAuthenticate: (action: () -> Unit) -> Unit
) {
    val colors = LocalRakshakXColors.current
    var showContent by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(vaultCategoryColor(entry.category).copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(vaultCategoryIcon(entry.category), null, tint = vaultCategoryColor(entry.category), modifier = Modifier.size(24.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                        Text(entry.category.name.replace("_", " "), style = MaterialTheme.typography.bodySmall, color = vaultCategoryColor(entry.category))
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, null, tint = colors.textMuted)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, null, tint = colors.textMuted)
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.4f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Content",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            if (!showContent) {
                                onAuthenticate { showContent = true }
                            } else {
                                showContent = false
                            }
                        }) {
                            Icon(
                                if (showContent) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showContent) "Hide" else "Reveal", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.cardBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (showContent) entry.content else "•".repeat(minOf(entry.content.length, 32)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (showContent) colors.textPrimary else colors.textMuted,
                            fontFamily = if (showContent && entry.category == VaultCategory.PASSWORD)
                                androidx.compose.ui.text.font.FontFamily.Monospace
                            else androidx.compose.ui.text.font.FontFamily.Default
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Created", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                        Text(formatDate(entry.createdAt), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Column {
                        Text("Updated", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                        Text(formatDate(entry.updatedAt), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

private fun vaultCategoryIcon(category: VaultCategory): ImageVector = when (category) {
    VaultCategory.PASSWORD -> Icons.Filled.VpnKey
    VaultCategory.NOTE -> Icons.Filled.Note
    VaultCategory.RECOVERY_CODE -> Icons.Filled.Key
    VaultCategory.API_KEY -> Icons.Filled.Code
    VaultCategory.CREDIT_CARD -> Icons.Filled.CreditCard
    VaultCategory.OTHER -> Icons.Filled.Lock
}

@Composable
private fun vaultCategoryColor(category: VaultCategory): Color {
    val colors = LocalRakshakXColors.current
    return when (category) {
        VaultCategory.PASSWORD -> colors.primary
        VaultCategory.NOTE -> Amber
        VaultCategory.RECOVERY_CODE -> Emerald
        VaultCategory.API_KEY -> Amethyst
        VaultCategory.CREDIT_CARD -> Crimson
        VaultCategory.OTHER -> colors.textSecondary
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
