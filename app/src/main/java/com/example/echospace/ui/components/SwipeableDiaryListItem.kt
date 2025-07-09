package com.example.echospace.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
//import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
//import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.ui.icons.CustomIcons
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

//@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableDiaryListItem(
    entry: DiaryEntry,
    onItemClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    isRevealed: Boolean,
    onRevealChange: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val actionButtonWidth = with(density) { 59.5.dp.toPx() } // Slightly smaller buttons
    val separatorWidth = with(density) { 1.dp.toPx() } // Tiny separator
    val outerPadding = with(density) { 8.dp.toPx() }
    val totalActionsWidth = (actionButtonWidth * 2) + separatorWidth + outerPadding

    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Sync with parent state
    LaunchedEffect(isRevealed) {
        val targetOffset = if (isRevealed) totalActionsWidth else 0f
        if (offsetX.value != targetOffset) {
            offsetX.animateTo(targetOffset, tween(200))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        // Background action buttons (stacked behind the file listing)
        Box(
            modifier = Modifier
                .width(129.dp)
                .fillMaxHeight()
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .zIndex(0f) // Ensure action buttons stay behind
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Start
            ) {
                // Rename button (left)
                Box(
                    modifier = Modifier
                        .width(59.5.dp)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(
                                topStart = 8.dp,
                                bottomStart = 8.dp,
                                topEnd = 0.dp,
                                bottomEnd = 0.dp
                            )
                        )
                        .clickable {
                            onRename()
                            onRevealChange(false)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CustomIcons.FormTextbox,
                        contentDescription = "Rename",
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(21.dp)
                    )
                }

                // Subtle separator
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                )

                // Delete button (right)
                Box(
                    modifier = Modifier
                        .width(59.5.dp)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(
                                topStart = 0.dp,
                                bottomStart = 0.dp,
                                topEnd = 8.dp,
                                bottomEnd = 8.dp
                            )
                        )
                        .clickable {
                            onDelete()
                            onRevealChange(false)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Main content with FIXED gesture handling
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .zIndex(1f)
                // Remove combinedClickable from here
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val shouldReveal = offsetX.value > totalActionsWidth * 0.5f
                                val targetOffset = if (shouldReveal) totalActionsWidth else 0f
                                offsetX.animateTo(targetOffset, tween(200))
                                onRevealChange(shouldReveal)
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + dragAmount).coerceIn(0f, totalActionsWidth)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                },
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(4.dp),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            DiaryListItem(
                entry = entry,
                onClick = onItemClick,
                onLongClick = {
                    if (offsetX.value < totalActionsWidth * 0.5f) {
                        // If hidden, reveal actions
                        coroutineScope.launch {
                            offsetX.animateTo(totalActionsWidth, tween(200))
                            onRevealChange(true)
                        }
                    }
                }
            )
        }
    }
}
