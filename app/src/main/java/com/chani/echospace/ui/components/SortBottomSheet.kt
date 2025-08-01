// ui/components/SortBottomSheet.kt
package com.chani.echospace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.chani.echospace.domain.model.SortOption
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    isDarkTheme: Boolean,
    currentSortOption: SortOption,
    onSortOptionSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        // This is the workaround you found
        val view = LocalView.current
        (view.parent as? DialogWindowProvider)?.window?.let { window ->
            SideEffect {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Sort Files",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // --- NEW: Two Columns for Sort Options ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Name column
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    SortOptionItem(
                        sortOption = SortOption.NAME_ASC,
                        isSelected = currentSortOption == SortOption.NAME_ASC,
                        onSelected = {
                            onSortOptionSelected(SortOption.NAME_ASC)
                            onDismiss()
                        }
                    )
                    SortOptionItem(
                        sortOption = SortOption.NAME_DESC,
                        isSelected = currentSortOption == SortOption.NAME_DESC,
                        onSelected = {
                            onSortOptionSelected(SortOption.NAME_DESC)
                            onDismiss()
                        }
                    )
                }
                // Date column
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    SortOptionItem(
                        sortOption = SortOption.DATE_NEWEST,
                        isSelected = currentSortOption == SortOption.DATE_NEWEST,
                        onSelected = {
                            onSortOptionSelected(SortOption.DATE_NEWEST)
                            onDismiss()
                        }
                    )
                    SortOptionItem(
                        sortOption = SortOption.DATE_OLDEST,
                        isSelected = currentSortOption == SortOption.DATE_OLDEST,
                        onSelected = {
                            onSortOptionSelected(SortOption.DATE_OLDEST)
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SortOptionItem(
    sortOption: SortOption,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelected,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.inverseOnSurface,
                unselectedColor = MaterialTheme.colorScheme.inverseOnSurface
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            imageVector = getSortIcon(sortOption),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.inverseOnSurface
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = sortOption.displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun getSortIcon(sortOption: SortOption): ImageVector {
    return when (sortOption) {
        SortOption.NAME_ASC -> Icons.Default.SortByAlpha
        SortOption.NAME_DESC -> Icons.Default.SortByAlpha
        SortOption.DATE_NEWEST -> Icons.Default.Schedule
        SortOption.DATE_OLDEST -> Icons.Default.Schedule
    }
}
