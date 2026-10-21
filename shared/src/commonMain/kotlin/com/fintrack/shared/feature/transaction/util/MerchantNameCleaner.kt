package com.fintrack.shared.feature.transaction.util

object MerchantNameCleaner {

    fun clean(raw: String): String {
        var text = raw.trim()
        if (text.isBlank()) return "Other"

        // 1. Strip (Ref: ...) or (Acc: ...) parenthetical suffixes
        text = text.replace(Regex("""(?i)\(ref[^)]*\)"""), "").trim()
        text = text.replace(Regex("""(?i)\(account[^)]*\)"""), "").trim()
        text = text.replace(Regex("""(?i)\(acc[^)]*\)"""), "").trim()
        text = text.replace(Regex("""[\(\)]"""), " ").trim()

        // 2. Strip transaction prefixes (Expenses & Income)
        val prefixes = listOf(
            "you have received", "funds received from", "money received from", "received from", "received",
            "deposit from", "payment from", "transfer from", "transferred from", "inflow from",
            "sent to", "paid to", "payment to", "transfer to", "transferred to",
            "pay bill to", "paybill to", "buy goods to", "bought from",
            "withdrawal from", "withdrawal at", "agent withdrawal at", "deposit at",
            "loan repayment to", "card payment at"
        )
        for (prefix in prefixes) {
            if (text.startsWith(prefix, ignoreCase = true)) {
                text = text.substring(prefix.length).trim()
                break
            }
        }

        // 3. Remove 10-digit / 12-digit phone numbers
        text = text.replace(Regex("""\b(254[71]\d{8}|0[71]\d{8})\b"""), "").trim()

        // 4. Remove Paybill / Till / Account / Ref prefixes & numbers
        text = text.replace(Regex("""(?i)\bfor\s+(account|acc|ount)\b"""), "").trim()
        text = text.replace(Regex("""(?i)\b(paybill|till|acc\.?|account|ount|ref\.?|reference|receipt)\s*#?\d*"""), "").trim()

        // 5. Remove trailing ref codes / numbers
        text = text.replace(Regex("""\b[A-Z0-9]{8,12}\b"""), "").trim()
        text = text.replace(Regex("""\b\d{5,12}\b"""), "").trim()

        // 6. Strip punctuation
        text = text.replace(":", "").replace(";", "").replace("?", "").replace("!", "")
        text = text.replace(Regex("""\s+"""), " ").trim('-', ' ', ',', '.', ':', ';', '(', ')')

        if (text.isBlank()) return "Other"

        // 7. Title case format
        val clean = text.split(" ")
            .map { word -> word.trim(':', ';', ',', '.', '-', '(', ')') }
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                if (word.length <= 3 && word.all { it.isLetter() && it.isUpperCase() }) word
                else word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

        val finalClean = clean
            .replace(Regex("""(?i)\bfor\s+ount\b"""), "")
            .replace(Regex("""(?i)\bount\b"""), "")
            .replace("()", "")
            .trim('-', ' ', ',', '.', ':', ';', '(', ')')

        return if (finalClean.isBlank()) "Other" else finalClean
    }
}
