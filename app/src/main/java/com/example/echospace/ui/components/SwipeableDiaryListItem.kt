package com.example.echospace.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val actionButtonWidth = with(density) { 59.5.dp.toPx() }
    val separatorWidth = with(density) { 1.dp.toPx() }
    val outerPadding = with(density) { 8.dp.toPx() }
    val totalWidth = (actionButtonWidth * 2) + separatorWidth + outerPadding

    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Target offset for animation (only used when not dragging)
    val targetOffset = if (isRevealed) totalWidth else 0f

    // Smoother animation with tween for more fluid motion
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDragging) offsetX else targetOffset,
        animationSpec = if (isDragging) {
            tween(0) // Instant during drag
        } else {
            tween(120) // Faster snap animation
        },
        label = "swipe_offset"
    )

    // Update offsetX when isRevealed changes from parent
    LaunchedEffect(isRevealed) {
        if (!isDragging) {
            offsetX = if (isRevealed) totalWidth else 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        ActionButtons(
            onRename = {
                onRename()
                onRevealChange(false)
            },
            onDelete = {
                onDelete()
                onRevealChange(false)
            }
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .zIndex(1f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            val shouldReveal = offsetX > totalWidth * 0.5f
                            onRevealChange(shouldReveal)
                        }
                    ) { _, dragAmount ->
                        if (dragAmount > 0 || offsetX > 0) {
                            offsetX = (offsetX + dragAmount).coerceIn(0f, totalWidth)
                        }
                    }
                },
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(4.dp)
        ) {
            DiaryListItem(
                entry = entry,
                onClick = onItemClick,
                onLongClick = {
                    if (offsetX < totalWidth * 0.5f) {
                        onRevealChange(true)
                    }
                }
            )
        }
    }
}


@Composable
private fun ActionButtons(
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(128.dp)
            .fillMaxHeight()
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        ActionButton(
            onClick = onRename,
            icon = CustomIcons.FormTextbox,
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
        )

        Spacer(modifier = Modifier.width(1.dp))

        ActionButton(
            onClick = onDelete,
            icon = Icons.Default.DeleteOutline,
            backgroundColor = MaterialTheme.colorScheme.error,
            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
        )
    }
}

@Composable
private fun ActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: androidx.compose.ui.graphics.Color,
    shape: RoundedCornerShape
) {
    Box(
        modifier = Modifier
            .width(59.5.dp)
            .fillMaxHeight()
            .background(backgroundColor, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (backgroundColor == MaterialTheme.colorScheme.error) {
                MaterialTheme.colorScheme.background
            } else {
                MaterialTheme.colorScheme.background
            },
            modifier = if (backgroundColor == MaterialTheme.colorScheme.error) {
                Modifier.size(22.dp)
            } else {
                Modifier.size(21.dp)
            }
        )
    }
}
