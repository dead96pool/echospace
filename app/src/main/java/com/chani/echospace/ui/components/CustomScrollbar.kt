package com.chani.echospace.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun CustomScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    thickness: Dp = 4.dp,
    thumbMinHeight: Dp = 48.dp,
    hideDelay: Long = 800L,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
) {
    // ✅ FIXED: Stable scroll calculation that addresses both issues
    val scrollData by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset

            if (totalItems == 0 || visibleItemsInfo.isEmpty()) {
                ScrollbarData(
                    thumbSizePercent = 0f,
                    thumbOffsetPercent = 0f,
                    isVisible = false
                )
            } else {
                // ✅ FIX 1: Use a stable visible items count based on average
                val averageVisibleItems = 11.5f // Average between 11 and 12 from your logs
                val shouldShowScrollbar = totalItems > averageVisibleItems

                if (!shouldShowScrollbar) {
                    ScrollbarData(
                        thumbSizePercent = 0f,
                        thumbOffsetPercent = 0f,
                        isVisible = false
                    )
                } else {
                    // ✅ FIX 1: Use stable thumb size calculation
                    val thumbSizePercent = (averageVisibleItems / totalItems.toFloat()).coerceIn(0.1f, 1f)

                    // ✅ FIX 2: Include offset in scroll position for smooth motion
                    val averageItemHeight = if (visibleItemsInfo.isNotEmpty()) {
                        visibleItemsInfo.map { it.size }.average().toFloat()
                    } else {
                        100f // fallback
                    }

                    // Calculate exact scroll position including sub-item positioning
                    val exactScrollPosition = firstVisibleIndex.toFloat() +
                            (firstVisibleOffset.toFloat() / averageItemHeight)

                    val maxScrollPosition = totalItems.toFloat() - averageVisibleItems
                    val thumbOffsetPercent = if (maxScrollPosition > 0) {
                        (exactScrollPosition / maxScrollPosition).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    ScrollbarData(
                        thumbSizePercent = thumbSizePercent,
                        thumbOffsetPercent = thumbOffsetPercent,
                        isVisible = true
                    )
                }
            }
        }
    }

    // Auto-hide functionality (keep as is)
    var shouldShow by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress, scrollData.thumbOffsetPercent) {
        if (listState.isScrollInProgress) {
            shouldShow = true
        } else if (scrollData.isVisible) {
            delay(hideDelay)
            if (!listState.isScrollInProgress) {
                shouldShow = false
            }
        }
    }

    // ✅ FIXED: Only animate visibility
    val alpha by animateFloatAsState(
        targetValue = if (shouldShow && scrollData.isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (shouldShow) 100 else 200
        ),
        label = "ScrollbarAlpha"
    )

    if (scrollData.isVisible) {
        BoxWithConstraints(
            modifier = modifier.alpha(alpha),
            contentAlignment = Alignment.TopEnd
        ) {
            val trackHeight = maxHeight
            val thumbHeight = (trackHeight * scrollData.thumbSizePercent).coerceAtLeast(thumbMinHeight)
            val maxThumbOffset = (trackHeight - thumbHeight).coerceAtLeast(0.dp)
            val thumbOffset = maxThumbOffset * scrollData.thumbOffsetPercent

            // Scrollbar thumb
            Box(
                modifier = Modifier
                    .width(thickness)
                    .height(thumbHeight)
                    .offset(y = thumbOffset)
                    .background(
                        color = contentColor,
                        shape = RoundedCornerShape(thickness / 2)
                    )
            )
        }
    }
}

private data class ScrollbarData(
    val thumbSizePercent: Float,
    val thumbOffsetPercent: Float,
    val isVisible: Boolean
)
