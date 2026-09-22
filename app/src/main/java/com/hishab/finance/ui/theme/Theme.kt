package com.hishab.finance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.hishab.finance.data.repository.ThemeMode

/** Colours Material 3 has no slot for, but which this app uses everywhere. */
data class MoneyColors(
    val income: Color,
    val expense: Color,
    val forecast: Color,
    val warning: Color,
    val safe: Color,
    val muted: Color,
    val cardStroke: Color,
    val heroStart: Color,
    val heroEnd: Color
) {
    val heroBrush: Brush get() = Brush.linearGradient(listOf(heroStart, heroEnd))
    fun directional(isIncome: Boolean): Color = if (isIncome) income else expense
}

private val LightMoney = MoneyColors(
    income = IncomeGreen, expense = ExpenseRose, forecast = ForecastAmber,
    warning = WarnAmber, safe = SafeTeal, muted = MutedLight,
    cardStroke = NeutralLightOutline, heroStart = Indigo600, heroEnd = Violet500
)

private val DarkMoney = MoneyColors(
    income = IncomeGreenDark, expense = ExpenseRoseDark, forecast = ForecastAmber,
    warning = WarnAmber, safe = SafeTeal, muted = MutedDark,
    cardStroke = NeutralDarkOutline, heroStart = Indigo500, heroEnd = Violet500
)

private val LocalMoneyColors = staticCompositionLocalOf { LightMoney }

object HishabTheme {
    val money: MoneyColors
        @Composable @ReadOnlyComposable get() = LocalMoneyColors.current
}

private val LightScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = IncomeGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = ForecastAmber,
    onTertiary = Color(0xFF3B2503),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = NeutralLightBg,
    onBackground = InkLight,
    surface = NeutralLightSurface,
    onSurface = InkLight,
    surfaceVariant = NeutralLightVariant,
    onSurfaceVariant = MutedLight,
    outline = NeutralLightOutline,
    outlineVariant = Color(0xFFE6EAF2),
    error = ExpenseRose,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337)
)

private val DarkScheme = darkColorScheme(
    primary = Indigo400,
    onPrimary = Color(0xFF15173A),
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = IncomeGreenDark,
    onSecondary = Color(0xFF00281B),
    secondaryContainer = Color(0xFF12503C),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = ForecastAmber,
    onTertiary = Color(0xFF3B2503),
    tertiaryContainer = Color(0xFF5A3A08),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = NeutralDarkBg,
    onBackground = InkDark,
    surface = NeutralDarkSurface,
    onSurface = InkDark,
    surfaceVariant = NeutralDarkVariant,
    onSurfaceVariant = MutedDark,
    outline = NeutralDarkOutline,
    outlineVariant = Color(0xFF24324C),
    error = ExpenseRoseDark,
    onError = Color(0xFF3A0A15),
    errorContainer = Color(0xFF5C1226),
    onErrorContainer = Color(0xFFFFE4E6)
)

@Composable
fun HishabTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalMoneyColors provides if (dark) DarkMoney else LightMoney) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = HishabTypography,
            content = content
        )
    }
}
