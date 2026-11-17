@file:Suppress("DEPRECATION")

package com.nowlsantos.aiconcierge.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = PeninsulaGold,
    onPrimary = BlackPen,
    secondary = PeninsulaGold,
    background = PeninsulaCharcoal,
    surface = PeninsulaDarkCard,
    onBackground = WhitePen,
    onSurface = WhitePen,
    secondaryContainer = PeninsulaDarkCard,
    onSecondaryContainer = PeninsulaGold
)

private val LightColorScheme = lightColorScheme(
    primary = PeninsulaGold,
    onPrimary = WhitePen,
    secondary = PeninsulaGold,
    background = PeninsulaIvory,
    surface = PeninsulaLightCard,
    onBackground = PeninsulaCharcoal,
    onSurface = PeninsulaCharcoal,
    secondaryContainer = PeninsulaLightCard,
    onSecondaryContainer = PeninsulaGold
)

@Composable
fun PeninsulaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = sfTypography,
        content = content
    )
}
