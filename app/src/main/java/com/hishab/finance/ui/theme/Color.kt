package com.hishab.finance.ui.theme

import androidx.compose.ui.graphics.Color

// Brand: a deep indigo that reads as "ledger", warmed by an amber used only for forecasts.
val Indigo600 = Color(0xFF4F46E5)
val Indigo500 = Color(0xFF6366F1)
val Indigo400 = Color(0xFF818CF8)
val Violet500 = Color(0xFF7C3AED)
val IndigoContainerLight = Color(0xFFE0E7FF)
val IndigoContainerDark = Color(0xFF2A2F6B)

// Money direction. These two never change meaning anywhere in the app.
val IncomeGreen = Color(0xFF10B981)
val IncomeGreenDark = Color(0xFF34D399)
val ExpenseRose = Color(0xFFF43F5E)
val ExpenseRoseDark = Color(0xFFFB7185)

// Forecast and budget states.
val ForecastAmber = Color(0xFFF59E0B)
val WarnAmber = Color(0xFFF97316)
val SafeTeal = Color(0xFF14B8A6)

val NeutralLightBg = Color(0xFFF5F6FA)
val NeutralLightSurface = Color(0xFFFFFFFF)
val NeutralLightVariant = Color(0xFFEDF0F7)
val NeutralLightOutline = Color(0xFFD7DCE8)
val InkLight = Color(0xFF111827)
val MutedLight = Color(0xFF64748B)

val NeutralDarkBg = Color(0xFF0B1220)
val NeutralDarkSurface = Color(0xFF141E33)
val NeutralDarkVariant = Color(0xFF1C2942)
val NeutralDarkOutline = Color(0xFF2C3A55)
val InkDark = Color(0xFFE8ECF5)
val MutedDark = Color(0xFF94A3B8)

/** Fallback palette for charts when a category has no colour of its own. */
val ChartPalette = listOf(
    Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFF43F5E),
    Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF22C55E),
    Color(0xFF3B82F6), Color(0xFFF97316), Color(0xFF14B8A6), Color(0xFFA855F7)
)
