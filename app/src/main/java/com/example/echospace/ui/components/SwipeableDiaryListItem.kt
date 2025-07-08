package com.example.echospace.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
//import androidx.compose.material.icons.filled.DriveFileRenameOutline
//import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.ui.icons.CustomIcons
import kotlin.math.roundToInt

@Composable
fun SwipeableDiaryListItem(
    entry: DiaryEntry,
    onItemClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    isRevealed: Boolean = false,
    onRevealChange: (Boolean) -> Unit = {}
) {
    val density = LocalDensity.current

    // Calculate the exact width needed for two action buttons
    val actionButtonWidth = with(density) { 59.5.dp.toPx() } // Slightly smaller buttons
    val separatorWidth = with(density) { 1.dp.toPx() } // Tiny separator
    val outerPadding = with(density) { 8.dp.toPx() }
    val totalActionsWidth = (actionButtonWidth * 2) + separatorWidth + outerPadding

    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Target offset for animation (only used when not dragging)
    val targetOffset = if (isRevealed) totalActionsWidth else 0f

    // Smoother animation with tween for more fluid motion
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDragging) offsetX else targetOffset,
        animationSpec = tween(
            durationMillis = if (isDragging) 0 else 200 // No animation during drag
        ),
        label = "swipe_offset"
    )

    // Update offsetX when isRevealed changes from parent
    LaunchedEffect(isRevealed) {
        if (!isDragging) {
            offsetX = if (isRevealed) totalActionsWidth else 0f
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

        // Main content (file listing) - slides like a stacked layer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .zIndex(1f) // File listing stays on top but without shadow
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            val shouldReveal = offsetX > totalActionsWidth * 0.5f
                            onRevealChange(shouldReveal)
                        }
                    ) { _, dragAmount ->
                        if (dragAmount > 0 || offsetX > 0) {
                            offsetX = (offsetX + dragAmount).coerceIn(0f, totalActionsWidth)
                        }
                    }
                },
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(4.dp),
            // Removed all shadow and elevation properties for flat, stacked appearance
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            DiaryListItem(entry = entry, onClick = onItemClick)
        }
    }
}
