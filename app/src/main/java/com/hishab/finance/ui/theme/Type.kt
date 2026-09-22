package com.hishab.finance.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * One family throughout. Amounts are the loudest thing on screen, so display and headline
 * styles are tight and heavy while body text stays calm and readable.
 */
private val sans = FontFamily.SansSerif

val HishabTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 34.sp,
        lineHeight = 40.sp, letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Bold, fontSize = 26.sp,
        lineHeight = 32.sp, letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
        lineHeight = 28.sp, letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp
    )
)
