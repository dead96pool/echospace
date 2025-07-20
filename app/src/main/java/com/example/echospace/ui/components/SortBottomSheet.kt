// ui/components/SortBottomSheet.kt
package com.example.echospace.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import com.example.echospace.domain.model.SortOption
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import android.util.Log

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
        modifier = modifier
    ) {
        // This is the workaround you found
        val view = LocalView.current
        (view.parent as? DialogWindowProvider)?.window?.let { window ->
            SideEffect {
                Log.d("BottomSheetThemeCheck", "isSystemInDarkTheme value is: $isDarkTheme")
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

            Column(modifier = Modifier.selectableGroup()) {
                SortOption.entries.forEach { sortOption ->
                    SortOptionItem(
                        sortOption = sortOption,
                        isSelected = currentSortOption == sortOption,
                        onSelected = {
                            onSortOptionSelected(sortOption)
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
            onClick = null
        )

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            imageVector = getSortIcon(sortOption),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface
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
