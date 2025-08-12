package com.chani.echospace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun SlimTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showBottomShadow: Boolean = true,
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {},
    onSortClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isRefreshing: Boolean,
    onSettingsClick: () -> Unit = { },
    isSearching: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchStart: () -> Unit = {},
    onSearchEnd: () -> Unit = {},
    isSearchLoading: Boolean = false,
    isVirtualMode: Boolean = false,
    currentVirtualPath: List<String> = emptyList(),
    onBreadcrumbClick: ((Int) -> Unit)? = null
) {
    var showDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding() // add the top bar below the android status bar
    ) {
        // Main top bar content
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                if (!isSearching) {
                    // Normal mode: Title + Search & Menu icons

                    // Title
                    if (isVirtualMode && currentVirtualPath.isNotEmpty()) {
                        // Show breadcrumb navigation
                        BreadcrumbNavigation(
                            virtualPath = currentVirtualPath,
                            onBreadcrumbClick = onBreadcrumbClick,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Show regular title
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Right side icons: Search + Menu
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search icon button
                        IconButton(
                            onClick = onSearchStart,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(25.dp)
                            )
                        }

                        // Three-dot menu button
                        Box {
                            IconButton(
                                onClick = { showDropdown = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(25.dp)
                                )
                            }

                            // Dropdown menu (same as before)
                            DropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                // Refresh option
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = if (isRefreshing) {
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Refresh",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    },
                                    onClick = {
                                        if (!isRefreshing) {
                                            onRefreshClick()
                                            showDropdown = false
                                        }
                                    },
                                    enabled = !isRefreshing
                                )

                                // Theme toggle option
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = if (isDarkTheme) "Light Mode" else "Dark Mode",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    },
                                    onClick = {
                                        onThemeToggle()
                                        showDropdown = false
                                    }
                                )

                                // Sort option
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Sort Files",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSortClick()
                                        showDropdown = false
                                    }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )

                                // Settings page option
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Settings",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSettingsClick() // Call the settings navigation
                                        showDropdown = false
                                    }
                                )
                            }
                        }
                    }

                } else {
                    // Search mode: Back arrow + Search TextField

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back arrow button
                        IconButton(
                            onClick = onSearchEnd,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        // Search TextField with proper focus management
                        val focusRequester = remember { FocusRequester() }
                        val keyboardController = LocalSoftwareKeyboardController.current

                        // Search TextField
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                //.weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text(
                                    text = "Search files and content...",
                                    style = MaterialTheme.typography.bodyLarge, // Smaller text style
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            singleLine = true,
                            // Keyboard options to show search icon
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search,
                                keyboardType = KeyboardType.Text
                            ),
                            // Handle search action when user taps search button
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    // The search is already handled by the debounced onSearchQueryChange
                                    // So we just need to hide the keyboard or perform any additional action
                                    keyboardController?.hide()
                                }
                            ),
                            trailingIcon = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Show loading spinner when searching
                                    if (isSearchLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Show clear button when there's text
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { onSearchQueryChange("") },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        // Automatic focus when search mode activates
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    }
                }
            }
        }

        // Bottom shadow effect
        if (showBottomShadow) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun BreadcrumbNavigation(
    virtualPath: List<String>,
    onBreadcrumbClick: ((Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Root "EchoSpace" breadcrumb
        if (virtualPath.isNotEmpty()) {
            Text(
                text = "EchoSpace",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.clickable {
                    onBreadcrumbClick?.invoke(-1) // -1 indicates root navigation
                }
            )
        }

        // Virtual folder breadcrumbs
        virtualPath.forEachIndexed { index, pathKey ->
            // Separator
            Text(
                text = " / ",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            // Breadcrumb segment
            val isLast = index == virtualPath.size - 1
            Text(
                text = formatDisplayName(pathKey), // Convert to camel case
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = if (isLast) 18.sp else 16.sp
                ),
                color = if (isLast) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
                modifier = if (!isLast && onBreadcrumbClick != null) {
                    Modifier.clickable {
                        onBreadcrumbClick(index)
                    }
                } else {
                    Modifier
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Convert virtual path key to display format with camel casing
 * This matches your MetadataUtils.formatDisplayName functionality
 */
private fun formatDisplayName(input: String): String {
    if (input.isBlank()) return input

    // Split by non-alphanumeric characters to handle compound words
    val words = input.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }

    return words.joinToString(" ") { word ->
        // Check if word is purely numeric
        if (word.all { it.isDigit() }) {
            word // Keep numbers as-is
        } else {
            // Apply camel case to text
            word.lowercase(Locale.getDefault()).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
    }
}
