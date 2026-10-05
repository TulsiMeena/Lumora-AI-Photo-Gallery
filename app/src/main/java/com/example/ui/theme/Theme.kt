package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class GalleryCustomColors(
  val background: Color,
  val surface: Color,
  val surfaceVariant: Color,
  val card: Color,
  val primaryText: Color,
  val secondaryText: Color,
  val tertiaryText: Color,
  val border: Color,
  val divider: Color,
  val accent: Color,
  val accentVariant: Color,
  val accentGlow: Color,
  val error: Color,
  val success: Color,
  val warning: Color,
  val info: Color,
  val isDark: Boolean
) {
  val textPrimary: Color get() = primaryText
  val textSecondary: Color get() = secondaryText
  val textTertiary: Color get() = tertiaryText
  val surfaceBackground: Color get() = background
  val surfaceCard: Color get() = card
  val cardBackground: Color get() = card
}

val LightCustomColors = GalleryCustomColors(
  background = GalleryLightBackground,
  surface = GalleryLightSurface,
  surfaceVariant = GalleryLightSurfaceVariant,
  card = GalleryLightCard,
  primaryText = GalleryLightTextPrimary,
  secondaryText = GalleryLightTextSecondary,
  tertiaryText = GalleryLightTextTertiary,
  border = GalleryLightBorder,
  divider = GalleryLightDivider,
  accent = GalleryAccent,
  accentVariant = GalleryAccentVariant,
  accentGlow = GalleryAccentGlow,
  error = GalleryError,
  success = GallerySuccess,
  warning = GalleryWarning,
  info = GalleryInfo,
  isDark = false
)

val DarkCustomColors = GalleryCustomColors(
  background = GalleryDarkBackground,
  surface = GalleryDarkSurface,
  surfaceVariant = GalleryDarkSurfaceVariant,
  card = GalleryDarkCard,
  primaryText = GalleryDarkTextPrimary,
  secondaryText = GalleryDarkTextSecondary,
  tertiaryText = GalleryDarkTextTertiary,
  border = GalleryDarkBorder,
  divider = GalleryDarkDivider,
  accent = GalleryAccent,
  accentVariant = GalleryAccentVariant,
  accentGlow = GalleryAccentGlow,
  error = GalleryError,
  success = GallerySuccess,
  warning = GalleryWarning,
  info = GalleryInfo,
  isDark = true
)

val LocalGalleryColors = staticCompositionLocalOf { LightCustomColors }
val LocalGalleryDimens = staticCompositionLocalOf { DefaultGalleryDimens }

object GalleryTheme {
  val colors: GalleryCustomColors
    @Composable
    @ReadOnlyComposable
    get() = LocalGalleryColors.current

  val dimens: GalleryDimens
    @Composable
    @ReadOnlyComposable
    get() = LocalGalleryDimens.current
}

private val DarkMaterialColorScheme = darkColorScheme(
  primary = GalleryAccent,
  onPrimary = Color.White,
  primaryContainer = GalleryDarkSurfaceVariant,
  onPrimaryContainer = GalleryDarkTextPrimary,
  secondary = GalleryPurple,
  onSecondary = Color.White,
  background = GalleryDarkBackground,
  onBackground = GalleryDarkTextPrimary,
  surface = GalleryDarkSurface,
  onSurface = GalleryDarkTextPrimary,
  surfaceVariant = GalleryDarkSurfaceVariant,
  onSurfaceVariant = GalleryDarkTextSecondary,
  outline = GalleryDarkBorder,
  error = GalleryError,
  onError = Color.White
)

private val LightMaterialColorScheme = lightColorScheme(
  primary = GalleryAccent,
  onPrimary = Color.White,
  primaryContainer = GalleryLightSurfaceVariant,
  onPrimaryContainer = GalleryLightTextPrimary,
  secondary = GalleryPurple,
  onSecondary = Color.White,
  background = GalleryLightBackground,
  onBackground = GalleryLightTextPrimary,
  surface = GalleryLightSurface,
  onSurface = GalleryLightTextPrimary,
  surfaceVariant = GalleryLightSurfaceVariant,
  onSurfaceVariant = GalleryLightTextSecondary,
  outline = GalleryLightBorder,
  error = GalleryError,
  onError = Color.White
)

enum class ThemeMode {
  SYSTEM, LIGHT, DARK
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val colorScheme: ColorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkMaterialColorScheme
    else -> LightMaterialColorScheme
  }

  val customColors = if (darkTheme) DarkCustomColors else LightCustomColors

  CompositionLocalProvider(
    LocalGalleryColors provides customColors,
    LocalGalleryDimens provides DefaultGalleryDimens
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = GalleryTypography,
      shapes = GalleryShapes,
      content = content
    )
  }
}
