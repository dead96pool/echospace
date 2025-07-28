package com.chani.echospace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
//import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
//import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SlimTopBar(
    title: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    showBottomShadow: Boolean = true,
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {},
    onSortClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isRefreshing: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()        // add the top bar below the android status bar
    ) {
        // Main top bar content
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )

                // Refresh button
                IconButton(
                    onClick = onRefreshClick,
                    enabled = !isRefreshing, // Disabled when refreshing
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isRefreshing) {
                            contentColor.copy(alpha = 0.38f) // Grayed out
                        } else {
                            contentColor
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Theme toggle button
                IconButton(
                    onClick = onThemeToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // sort button
                IconButton(
                    onClick = onSortClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort Files",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
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
