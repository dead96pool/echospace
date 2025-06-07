// ui/components/SlimTopAppBar.kt
package com.example.echospace.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
//import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SlimTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(backgroundColor),
    isSearchMode: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchClose: () -> Unit = {},
    searchFocusRequester: FocusRequester? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars), // Add status bar padding
        color = backgroundColor,
        contentColor = contentColor
    ) {
        if (isSearchMode) {
            // Search Mode Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(51.dp)
                    .padding(horizontal = 8.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField( // Use TextField instead of OutlinedTextField
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Search files and content...",
                            style = MaterialTheme.typography.bodyLarge, // Smaller text style
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (searchFocusRequester != null) {
                                Modifier.focusRequester(searchFocusRequester)
                            } else {
                                Modifier
                            }
                        ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    // Clear the search text if there's text
                                    onSearchQueryChange("")
                                } else {
                                    // Exit search mode if text field is empty
                                    onSearchClose()
                                }
                            },
                            modifier = Modifier.size(32.dp) // size of the touch area
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotEmpty()) {
                                    Icons.Default.Clear // Clear icon when there's text
                                } else {
                                    Icons.AutoMirrored.Filled.ArrowBack // Back arrow when empty
                                },
                                contentDescription = if (searchQuery.isNotEmpty()) "Clear search" else "Close search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = if (searchQuery.isNotEmpty()) 1f else 0.7f // More prominent when there's text
                                ),
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                )
            }
        } else {
            // Normal Mode Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(51.dp)
                    .padding(horizontal = 15.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    navigationIcon()
                    title()
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions()
                }
            }
        }
    }
}