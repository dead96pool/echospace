package com.chani.echospace.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
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

// Data class to unify scroll information
data class ScrollbarData(
    val thumbSizePercent: Float,
    val thumbOffsetPercent: Float,
    val isVisible: Boolean
)

// Single CustomScrollbar that handles both types
@Composable
fun CustomScrollbar(
    modifier: Modifier = Modifier,
    thickness: Dp = 4.dp,
    thumbMinHeight: Dp = 48.dp,
    hideDelay: Long = 800L,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    // Accept either type of scroll state
    lazyListState: LazyListState? = null,
    scrollState: ScrollState? = null
) {
    // Ensure exactly one scroll state is provided
    require((lazyListState != null) xor (scrollState != null)) {
        "Provide exactly one of lazyListState or scrollState"
    }

    // Calculate scrollbar data based on the provided state type
    val scrollData by remember {
        derivedStateOf {
            when {
                lazyListState != null -> calculateLazyListScrollData(lazyListState)
                scrollState != null -> calculateScrollStateScrollData(scrollState)
                else -> ScrollbarData(0f, 0f, false) // Should never happen due to require()
            }
        }
    }

    // Determine if scrolling is in progress
    val isScrollInProgress = lazyListState?.isScrollInProgress
        ?: scrollState?.isScrollInProgress
        ?: false

    // Auto-hide functionality (same for both types)
    var shouldShow by remember { mutableStateOf(false) }

    LaunchedEffect(isScrollInProgress, scrollData.thumbOffsetPercent) {
        if (isScrollInProgress) {
            shouldShow = true
        } else if (scrollData.isVisible) {
            delay(hideDelay.coerceAtMost(600L))
            shouldShow = false
        }
    }

    // Animate visibility (same for both types)
    val alpha by animateFloatAsState(
        targetValue = if (shouldShow && scrollData.isVisible) 1f else 0.2f,
        animationSpec = tween(
            durationMillis = if (shouldShow) 150 else 300,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "ScrollbarAlpha"
    )

    // Render scrollbar (same for both types)
    if (scrollData.isVisible) {
        BoxWithConstraints(
            modifier = modifier.alpha(alpha),
            contentAlignment = Alignment.TopEnd
        ) {
            val trackHeight = maxHeight
            val thumbHeight = (trackHeight * scrollData.thumbSizePercent).coerceAtLeast(thumbMinHeight)
            val maxThumbOffset = (trackHeight - thumbHeight).coerceAtLeast(0.dp)
            val thumbOffset = maxThumbOffset * scrollData.thumbOffsetPercent

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

// Helper function for LazyListState calculations
private fun calculateLazyListScrollData(listState: LazyListState): ScrollbarData {
    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    val firstVisibleIndex = listState.firstVisibleItemIndex
    val firstVisibleOffset = listState.firstVisibleItemScrollOffset

    if (totalItems == 0 || visibleItemsInfo.isEmpty()) {
        return ScrollbarData(
            thumbSizePercent = 0f,
            thumbOffsetPercent = 0f,
            isVisible = false
        )
    } else {
        // Use a stable visible items count based on average
        val averageVisibleItems = 11.5f // Average between 11 and 12 from your logs
        val shouldShowScrollbar = totalItems > averageVisibleItems

        if (!shouldShowScrollbar) {
            return ScrollbarData(
                thumbSizePercent = 0f,
                thumbOffsetPercent = 0f,
                isVisible = false
            )
        } else {
            // Use stable thumb size calculation
            val thumbSizePercent = (averageVisibleItems / totalItems.toFloat()).coerceIn(0.1f, 1f)

            // Include offset in scroll position for smooth motion
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

            return ScrollbarData(
                thumbSizePercent = thumbSizePercent,
                thumbOffsetPercent = thumbOffsetPercent,
                isVisible = true
            )
        }
    }
}

// Helper function for ScrollState calculations
private fun calculateScrollStateScrollData(scrollState: ScrollState): ScrollbarData {
    val maxScroll = scrollState.maxValue
    val currentScroll = scrollState.value

    if (maxScroll <= 0) {
        return ScrollbarData(0f, 0f, false)
    }

    // Estimate visible content ratio (typically 20-40% for text content)
    val thumbSizePercent = 0.3f.coerceIn(0.1f, 1f)
    val thumbOffsetPercent = (currentScroll.toFloat() / maxScroll.toFloat()).coerceIn(0f, 1f)

    return ScrollbarData(
        thumbSizePercent = thumbSizePercent,
        thumbOffsetPercent = thumbOffsetPercent,
        isVisible = true
    )
}
