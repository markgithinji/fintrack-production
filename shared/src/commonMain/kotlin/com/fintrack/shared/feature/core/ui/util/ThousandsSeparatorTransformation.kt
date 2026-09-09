package com.fintrack.shared.feature.core.ui.util

class ThousandsSeparatorOffsetMapping(private val originalText: String) {
    private val parts = originalText.split(".")
    private val integerPart = parts[0]

    fun originalToTransformed(offset: Int): Int {
        if (offset <= 0) return 0
        val safeOffset = offset.coerceAtMost(originalText.length)
        
        if (safeOffset > integerPart.length) {
            val commasInInteger = if (integerPart.length > 0) (integerPart.length - 1) / 3 else 0
            return safeOffset + commasInInteger
        }
        
        val digitsToRight = integerPart.length - safeOffset
        val totalCommas = if (integerPart.length > 0) (integerPart.length - 1) / 3 else 0
        val commasToRight = digitsToRight / 3
        val commasToLeft = totalCommas - commasToRight
        return safeOffset + commasToLeft
    }

    fun transformedToOriginal(offset: Int): Int {
        if (offset <= 0) return 0
        
        val integerPartRev = integerPart.reversed().chunked(3).joinToString(",").reversed()
        val decimalPart = if (parts.size > 1) "." + parts[1] else ""
        val formatted = (if (integerPartRev.isEmpty() && decimalPart.isNotEmpty()) "0" else integerPartRev) + decimalPart
        
        val safeOffset = offset.coerceAtMost(formatted.length)
        
        var commas = 0
        var foundDot = false
        for (i in 0 until safeOffset) {
            if (formatted[i] == ',') commas++
            if (formatted[i] == '.') {
                foundDot = true
                break
            }
        }
        
        return if (foundDot && offset > formatted.indexOf('.')) {
            val commasInInteger = if (integerPart.length > 0) (integerPart.length - 1) / 3 else 0
            offset - commasInInteger
        } else {
            offset - commas
        }
    }
}
