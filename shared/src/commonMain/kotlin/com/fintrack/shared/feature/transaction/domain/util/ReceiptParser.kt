package com.fintrack.shared.feature.transaction.domain.util

import kotlinx.datetime.LocalDate

object ReceiptParser {

    /**
     * Parses the raw text extracted from a receipt to find the likely total amount.
     * Looks for keywords like "total", "amount due", etc., and extracts the largest monetary value near them.
     */
    fun parseAmount(rawText: String): Double? {
        val lines = rawText.lowercase().split("\n")
        var maxAmount = 0.0
        var foundAmount = false

        // Common regex to match prices (e.g., 12.34, 1,234.56, $12.34)
        val priceRegex = Regex("""[$€£]?\s*(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2}))""")

        for (line in lines) {
            val matchResults = priceRegex.findAll(line)
            for (match in matchResults) {
                // Remove commas and dollar signs, convert comma to dot for parsing
                val cleanedStr = match.groupValues[1]
                    .replace(",", "")
                    .replace("$", "")
                    .replace("€", "")
                    .replace("£", "")
                    .trim()
                
                try {
                    val amount = cleanedStr.toDouble()
                    // Receipts often have many numbers, we usually want the largest one as the total.
                    if (amount > maxAmount) {
                        maxAmount = amount
                        foundAmount = true
                    }
                } catch (e: NumberFormatException) {
                    // Ignore parsing errors for individual matches
                }
            }
        }
        
        return if (foundAmount) maxAmount else null
    }

    /**
     * Parses the raw text to find a date.
     * Looks for common date formats.
     */
    fun parseDate(rawText: String): LocalDate? {
        // Simple regex for YYYY-MM-DD or MM/DD/YYYY or DD/MM/YYYY
        val dateRegex = Regex("""(\d{1,4})[-/.](\d{1,2})[-/.](\d{1,4})""")
        
        val match = dateRegex.find(rawText) ?: return null
        
        try {
            val part1 = match.groupValues[1].toInt()
            val part2 = match.groupValues[2].toInt()
            val part3 = match.groupValues[3].toInt()

            var year = 0
            var month = 0
            var day = 0

            // Rough heuristic to determine order based on length and common sense
            if (part1 > 1000) {
                year = part1
                month = part2
                day = part3
            } else if (part3 > 1000) {
                year = part3
                // US vs rest of world ambiguity here. Assume US MM/DD/YYYY for now if part1 <= 12
                if (part1 <= 12 && part2 > 12) {
                    month = part1
                    day = part2
                } else {
                    // Assume DD/MM/YYYY
                    day = part1
                    month = part2
                }
            } else {
                return null // Too ambiguous or just 2-digit years
            }
            
            // Basic validation
            if (month !in 1..12 || day !in 1..31) return null

            return LocalDate(year, month, day)
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Attempts to find the merchant name, usually by taking the first prominent line of text.
     */
    fun parseMerchant(rawText: String): String? {
        val lines = rawText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null
        
        // Return the first non-empty line that doesn't look like a date or a number
        for (line in lines) {
             if (line.length > 3 && !line.matches(Regex(".*\\d{2,}.*"))) {
                 return line
             }
        }
        
        return lines.firstOrNull()
    }

    /**
     * Checks if the receipt contains keywords indicating it's an M-Pesa transaction.
     */
    fun isMpesa(rawText: String): Boolean {
        val lower = rawText.lowercase()
        return lower.contains("m-pesa") || 
               lower.contains("mpesa") || 
               lower.contains("safaricom") || 
               lower.contains("paybill") || 
               lower.contains("buy goods") ||
               lower.contains("till number")
    }

    /**
     * Looks for common merchant or industry keywords to suggest a likely category name.
     */
    fun guessCategoryName(rawText: String, merchantName: String? = null): String? {
        val lowerText = rawText.lowercase()
        val lowerMerchant = merchantName?.lowercase() ?: ""
        
        // Combine text to search, giving priority to merchant name matches later if needed,
        // but for now, we just scan everything for keywords.
        val searchString = "$lowerMerchant $lowerText"

        return when {
            searchString.contains("supermarket") || 
            searchString.contains("naivas") || 
            searchString.contains("carrefour") || 
            searchString.contains("quickmart") || 
            searchString.contains("cleanshelf") || 
            searchString.contains("groceries") || 
            searchString.contains("foodplus") -> "Groceries"
            
            searchString.contains("restaurant") || 
            searchString.contains("kfc") || 
            searchString.contains("java") || 
            searchString.contains("artcaffe") || 
            searchString.contains("cafe") || 
            searchString.contains("food") || 
            searchString.contains("eatery") || 
            searchString.contains("pizza") || 
            searchString.contains("burger") -> "Food"
            
            searchString.contains("uber") || 
            searchString.contains("bolt") || 
            searchString.contains("little cab") || 
            searchString.contains("taxi") || 
            searchString.contains("fuel") || 
            searchString.contains("shell") || 
            searchString.contains("total") || 
            searchString.contains("rubis") || 
            searchString.contains("petrol") -> "Transport"
            
            searchString.contains("pharmacy") || 
            searchString.contains("goodlife") || 
            searchString.contains("hospital") || 
            searchString.contains("clinic") || 
            searchString.contains("health") || 
            searchString.contains("chemist") -> "Health"
            
            searchString.contains("kplc") || 
            searchString.contains("token") || 
            searchString.contains("water") || 
            searchString.contains("zuku") || 
            searchString.contains("dstv") || 
            searchString.contains("gotv") || 
            searchString.contains("safaricom home") -> "Bills"
            
            searchString.contains("airtime") || 
            searchString.contains("bundles") || 
            searchString.contains("safaricom offers") || 
            searchString.contains("data bundle") -> "Airtime"
            
            searchString.contains("netflix") || 
            searchString.contains("spotify") || 
            searchString.contains("showmax") || 
            searchString.contains("cinema") || 
            searchString.contains("movie") -> "Entertainment"
            
            searchString.contains("jigg") || 
            searchString.contains("jumia") || 
            searchString.contains("kilimall") || 
            searchString.contains("copia") || 
            searchString.contains("amazon") || 
            searchString.contains("shopping") -> "Shopping"
            
            else -> null
        }
    }
}
