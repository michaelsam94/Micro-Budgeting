package com.michael.microbudgeting.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = BrandTealLight,
    onPrimary = BrandInk,
    primaryContainer = BrandTeal,
    onPrimaryContainer = Color.White,
    secondary = BrandGold,
    onSecondary = BrandInk,
    secondaryContainer = BrandGoldDark,
    onSecondaryContainer = Color.White,
    tertiary = BrandGold,
    onTertiary = BrandInk,
    background = Color(0xFF071F21),
    onBackground = Color(0xFFEFFAF8),
    surface = Color(0xFF0B2A2D),
    onSurface = Color(0xFFEFFAF8),
    surfaceVariant = Color(0xFF173F43),
    onSurfaceVariant = Color(0xFFC3D8D5),
    outline = Color(0xFF6F918D)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrandTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F4EF),
    onPrimaryContainer = BrandInk,
    secondary = BrandGoldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8B7),
    onSecondaryContainer = BrandInk,
    tertiary = BrandGold,
    onTertiary = BrandInk,
    tertiaryContainer = Color(0xFFFFF2CC),
    onTertiaryContainer = BrandInk,
    background = Color(0xFFF6FBFA),
    onBackground = BrandInk,
    surface = Color(0xFFFFFBFE),
    onSurface = BrandInk,
    surfaceVariant = Color(0xFFE2EEEC),
    onSurfaceVariant = Color(0xFF526B68),
    outline = Color(0xFF8AA29F)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
