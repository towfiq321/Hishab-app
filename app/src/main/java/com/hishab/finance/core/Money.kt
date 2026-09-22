package com.hishab.finance.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Currency helpers. The app is single-currency (Bangladeshi Taka) by design, so every
 * amount in the database is a plain [Double] of Taka and formatting happens only at the edge.
 */
const val TAKA = "\u09F3" // ৳

private val symbols = DecimalFormatSymbols(Locale.US)
private val whole = DecimalFormat("#,##0", symbols)
private val precise = DecimalFormat("#,##0.##", symbols)

/** `12500.0` -> `৳12,500`. Pass [decimals] when paisa matters (rare in day to day use). */
fun Double.toTaka(decimals: Boolean = false, withSymbol: Boolean = true): String {
    val formatter = if (decimals) precise else whole
    val sign = if (this < 0) "-" else ""
    val prefix = if (withSymbol) TAKA else ""
    return sign + prefix + formatter.format(abs(this))
}

/** Signed version used on transaction rows: `+৳500` / `-৳500`. */
fun Double.toSignedTaka(isIncome: Boolean): String =
    (if (isIncome) "+" else "-") + abs(this).toTaka()

/** Short form for chart axes and tight cards: `৳12.5k`, `৳1.25L`. */
fun Double.toCompactTaka(): String {
    val v = abs(this)
    val body = when {
        v >= 10_000_000 -> precise.format(v / 10_000_000) + "Cr"
        v >= 100_000 -> precise.format(v / 100_000) + "L"
        v >= 1_000 -> precise.format(v / 1_000) + "k"
        else -> whole.format(v)
    }
    return (if (this < 0) "-" else "") + TAKA + body
}

/** Percentage change from [previous] to this value. Returns null when there is no base to compare. */
fun Double.percentChangeFrom(previous: Double): Double? {
    if (previous <= 0.0) return null
    return (this - previous) / previous * 100.0
}

fun Double.roundTaka(): Double = this.roundToLong().toDouble()

/** Rounds a forecast to a friendly number so ranges read as estimates, not false precision. */
fun Double.roundToNearest(step: Int): Double =
    if (step <= 0) this else (this / step).roundToLong().toDouble() * step
