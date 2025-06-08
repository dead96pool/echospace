// ui/theme/Theme.kt
package com.example.echospace.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Custom grey colors
private val DarkGrey = Color(0xFF424242)
private val LightGrey = Color(0xFF757575)
private val MediumGrey = Color(0xFF616161)
private val VeryLightGrey = Color(0xFFF5F5F5)

private val Headingcolor = Color(0xFF24a3e7)

private val DarkColorScheme = darkColorScheme(
    //primary = Color(0xFFBB86FC),
    primary = VeryLightGrey,
    secondary = Color(0xFF03DAC6),
    tertiary = Headingcolor,
    background = DarkGrey,
    surface = Color(0xFF2D2D2D),
    onBackground = Color.White,
    onSurface = Color.White,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    surfaceVariant = MediumGrey,
    onSurfaceVariant = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    //primary = Color(0xFF6200EE),
    primary = DarkGrey,
    secondary = Color(0xFF03DAC6),
    tertiary = Headingcolor,
    background = Color.White,
    surface = VeryLightGrey,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onPrimary = Color.White,
    onSecondary = Color.White,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = LightGrey
)

@Composable
fun EchoSpaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
