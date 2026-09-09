package com.fintrack.shared.feature.core.util

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.RoundingMode

/**
 * Formats a BigDecimal to a single decimal place if necessary.
 * Useful for percentages and rates.
 */
fun BigDecimal.formatToSinglePrecision(): String {
    val rounded = this.roundToDigitPositionAfterDecimalPoint(1, RoundingMode.ROUND_HALF_AWAY_FROM_ZERO)
    val s = rounded.toPlainString()
    return if (s.contains(".") && s.endsWith("0")) {
        val trimmed = s.trimEnd('0').trimEnd('.')
        trimmed
    } else {
        s
    }
}

/**
 * Formats a BigDecimal to two decimal places.
 */
fun BigDecimal.formatToTwoPrecision(): String {
    val rounded = this.roundToDigitPositionAfterDecimalPoint(2, RoundingMode.ROUND_HALF_AWAY_FROM_ZERO)
    val s = rounded.toPlainString()
    val parts = s.split(".")
    val integerPart = parts[0]
    val decimalPart = if (parts.size > 1) parts[1].padEnd(2, '0').take(2) else "00"
    return "$integerPart.$decimalPart"
}

fun BigDecimal.formatToCurrency(symbol: String = "KSh", showDecimals: Boolean = true): String {
    return "$symbol ${this.formatToAmount(showDecimals = showDecimals)}"
}

/**
 * Primary formatting tool for currency amounts.
 * Always rounds to 2 decimal places (cents) to avoid "crypto-like" long numbers.
 */
fun BigDecimal.formatToAmount(showDecimals: Boolean = true): String {
    val isNegative = this.signum() == -1
    val absValue = if (isNegative) this.negate() else this
    
    // Round to 2 decimal places for cents
    val rounded = absValue.roundToDigitPositionAfterDecimalPoint(2, RoundingMode.ROUND_HALF_AWAY_FROM_ZERO)
    
    val s = rounded.toPlainString()
    val parts = s.split(".")
    val integerPart = parts[0]
    val decimalPart = if (parts.size > 1) parts[1].padEnd(2, '0').take(2) else "00"
    
    val integerString = integerPart
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
    
    return if (showDecimals) {
        val result = "$integerString.$decimalPart"
        if (isNegative) "-$result" else result
    } else {
        if (isNegative) "-$integerString" else integerString
    }
}

fun BigDecimal.toDouble(): Double = try {
    this.toPlainString().toDouble()
} catch (e: Exception) {
    0.0
}

fun BigDecimal.toInt(): Int = this.toDouble().toInt()

fun Double.formatToCurrency(symbol: String = "KSh", showDecimals: Boolean = true): String = try { BigDecimal.fromDouble(this).formatToCurrency(symbol, showDecimals) } catch(e: Exception) { "$symbol $this" }
fun Double.formatToAmount(showDecimals: Boolean = true): String = try { BigDecimal.fromDouble(this).formatToAmount(showDecimals) } catch(e: Exception) { this.toString() }
