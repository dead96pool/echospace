package com.chani.echospace.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chani.echospace.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val folderPath by viewModel.folderPath.collectAsState()
    val exportPath by viewModel.exportPath.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportError by viewModel.exportError.collectAsState()
    val showExportError by viewModel.showExportError.collectAsState()
    val exportSuccess by viewModel.exportSuccess.collectAsState()
    val showSuccessIcon by viewModel.showSuccessIcon.collectAsState()
    val lastExportTimestamp by viewModel.lastExportTimestamp.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle export results
    LaunchedEffect(exportError) {
        exportError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearExportError()
        }
    }

    LaunchedEffect(exportSuccess) {
        exportSuccess?.let { success ->
            snackbarHostState.showSnackbar(
                message = "Export successful: ${success.fullPath}",
                duration = SnackbarDuration.Short
            )
            viewModel.clearExportSuccess()
        }
    }

    // Handle system back button to navigate back to home screen
    BackHandler(
        enabled = true,
        onBack = onBackClick
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { data ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 6.dp,
                            shadowElevation = 6.dp,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .wrapContentWidth()
                                .widthIn(max = 360.dp)
                                .defaultMinSize(minWidth = 160.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            Text(
                                text = data.visuals.message,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Storage Section
            SettingsSection(title = "Storage") {
                FolderPathCard(folderPath = folderPath)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // App Info Section
            SettingsSection(title = "About") {
                AppInfoCard()
            }
            Spacer(modifier = Modifier.height(24.dp))


            // Export Section
            SettingsSection(
                title = "Export",
                lastExportText = lastExportTimestamp
            ) {
                ExportSection(
                    exportPath = exportPath,
                    isExporting = isExporting,
                    showExportError = showExportError,
                    showSuccessIcon = showSuccessIcon,
                    onExport = {
                        viewModel.exportFiles()
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    lastExportText: String? = null,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        // Show last export timestamp if available
        if (lastExportText != null) {
            Column {
                Text(
                    text = buildAnnotatedString {
                        // Style just the "Last Export:" part
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize
                            )
                        ) {
                            append("Last Export: ")
                        }

                        // Keep original styling for the timestamp
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Normal,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize
                            )
                        ) {
                            append(lastExportText)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        content()
    }
}

@Composable
private fun ExportSection(
    exportPath: String?,
    isExporting: Boolean,
    showExportError: Boolean,
    showSuccessIcon: Boolean,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Export path card
        Card(
            modifier = Modifier
                .height(80.dp)
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Export Location",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    if (exportPath != null) {
                        Text(
                            text = exportPath,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    } else {
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // export button
        Card(
            modifier = Modifier.size(80.dp), // Square card
            colors = CardDefaults.cardColors(
                containerColor = when {
                    // Show green background during success (2 seconds)
                    showSuccessIcon -> SystemGreen
                    // Show red background during error (2 seconds)
                    showExportError -> SystemError
                    // Default background
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        enabled = !isExporting && exportPath != null,
                        onClick = { onExport() },
                        indication = ripple(bounded = true),
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    // Show loading indicator during export
                    isExporting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Show DoneAll icon for successful export (2 seconds)
                    showSuccessIcon -> {
                        Icon(
                            imageVector = Icons.Rounded.DoneAll,
                            contentDescription = "Export Successful",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Show archive icon (default state)
                    else -> {
                        Icon(
                            imageVector = Icons.Rounded.Archive,
                            contentDescription = "Export",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderPathCard(folderPath: String?) {
    Card(
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Storage Location",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (folderPath != null) {
                    Text(
                        text = folderPath,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "EchoSpace",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Version 1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}
