// /app/src/main/java/com/example/echospace/ui/theme/Theme.kt

package com.example.echospace.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light theme: Dark grey UI with white text
private val LightColorScheme = lightColorScheme(
    // Primary colors - used for main UI elements like FAB, buttons
    primary = DarkGrey,
    onPrimary = TextWhite,
    primaryContainer = MediumGrey,
    onPrimaryContainer = TextWhite,

    // Secondary colors - used for secondary UI elements
    secondary = MediumGrey,
    onSecondary = TextWhite,
    secondaryContainer = LightGrey,
    onSecondaryContainer = TextWhite,

    // Tertiary colors - used for accent elements
    tertiary = LightGrey,
    onTertiary = TextWhite,
    tertiaryContainer = DarkGrey,
    onTertiaryContainer = TextWhite,

    // Background colors - main app background stays white for text readability
    background = AlmostWhite,
    onBackground = TextBlack,

    // Surface colors - for cards, dialogs, etc.
    surface = TextWhite,
    onSurface = TextBlack,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = TextBlack,

    // System colors
    error = SystemRed,
    onError = TextWhite,
    outline = LightGrey,
    outlineVariant = Color(0xFFE0E0E0)
)

// Dark theme: Almost white UI with dark text
private val DarkColorScheme = darkColorScheme(
    // Primary colors - used for main UI elements like FAB, buttons
    primary = AlmostWhite,
    onPrimary = TextBlack,
    primaryContainer = LightWhite,
    onPrimaryContainer = TextBlack,

    // Secondary colors - used for secondary UI elements
    secondary = LightWhite,
    onSecondary = TextBlack,
    secondaryContainer = MediumWhite,
    onSecondaryContainer = TextBlack,

    // Tertiary colors - used for accent elements
    tertiary = MediumWhite,
    onTertiary = TextBlack,
    tertiaryContainer = AlmostWhite,
    onTertiaryContainer = TextBlack,

    // Background colors - main app background stays dark for dark theme
    background = DarkGrey,
    onBackground = TextWhite,

    // Surface colors - for cards, dialogs, etc.
    surface = Color(0xFF1E1E1E),
    onSurface = TextWhite,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = TextWhite,

    // System colors
    error = SystemRed,
    onError = TextBlack,
    outline = MediumWhite,
    outlineVariant = Color(0xFF404040)
)

@Composable
fun EchoSpaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            // Handle system bar styling based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                // Android 15+ - Use edge-to-edge approach
                // System bars become transparent, content draws behind them
                WindowCompat.setDecorFitsSystemWindows(window, false)

                // Set appearance for system bar icons/text
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme

            } else {
                // Android 14 and below - Use traditional approach
                @Suppress("DEPRECATION")
                window.statusBarColor = if (darkTheme) {
                    Color(0xFF121212).toArgb()
                } else {
                    TextWhite.toArgb()
                }

                @Suppress("DEPRECATION")
                window.navigationBarColor = if (darkTheme) {
                    Color(0xFF121212).toArgb()
                } else {
                    TextWhite.toArgb()
                }

                // Set icon colors for system bars
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
