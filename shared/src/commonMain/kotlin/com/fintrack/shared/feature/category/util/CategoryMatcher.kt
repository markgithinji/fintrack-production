package com.fintrack.shared.feature.category.util

import com.fintrack.shared.feature.category.domain.model.Category
import com.fintrack.shared.feature.category.domain.model.CategoryRule

object CategoryMatcher {

    /**
     * Resolves a category ID based on provided inputs and existing categories.
     * Mirrors the backend fuzzy matching logic for Safaricom/Equity descriptions.
     */
    fun resolveCategory(
        inputCategoryId: String?,
        inputCategoryName: String?,
        description: String?,
        isIncome: Boolean,
        allCategories: List<Category>,
        rules: List<CategoryRule> = emptyList(),
        defaultId: String = "pending"
    ): String {
        // 1. Valid ID Check
        if (!inputCategoryId.isNullOrBlank() && inputCategoryId != "pending") {
            return inputCategoryId
        }

        val rawInput = if (inputCategoryName?.lowercase() == "pending") null else inputCategoryName
        val textToMatch = (rawInput ?: description ?: "").lowercase()
        if (textToMatch.isBlank()) return defaultId

        val isExpense = !isIncome

        // 2. Dynamic Rule Matching
        rules.filter { it.isExpense == isExpense }.forEach { rule ->
            val keyword = rule.keyword.lowercase()
            if (textToMatch.contains(keyword) || (description?.lowercase()?.contains(keyword) == true)) {
                return rule.categoryId
            }
        }

        val normalizedInput = textToMatch.replace("-", "").trim()

        // 3. Direct match with type
        allCategories.find {
            it.name.equals(textToMatch, ignoreCase = true) && it.isExpense == isExpense
        }?.let { return it.id }

        // 4. Direct match without type (fallback)
        allCategories.find {
            it.name.equals(textToMatch, ignoreCase = true)
        }?.let { return it.id }

        // 5. Fuzzy matching for variations
        allCategories.find {
            val norm = it.name.replace("-", "").trim().lowercase()
            norm == normalizedInput || norm.startsWith(normalizedInput) || normalizedInput.startsWith(norm)
        }?.let { return it.id }

        // 6. Safaricom / M-Pesa Specific Aliases (Backend Port)
        if (normalizedInput.contains("shwari") || normalizedInput.contains("saving")) {
            allCategories.find { it.name.contains("Savings", ignoreCase = true) }?.let { return it.id }
        }
        if (normalizedInput.contains("loan")) {
            allCategories.find { it.name.contains("Loans", ignoreCase = true) }?.let { return it.id }
        }

        // 7. Hardcoded Fallbacks (Offline Compatibility)
        val hardcodedId = matchHardcodedKeyword(normalizedInput, isIncome, allCategories)
        if (hardcodedId != null) return hardcodedId

        // 8. Final Fallback
        val fallbackName = if (isExpense) "Transfer" else "Other Income"
        return allCategories.find { it.name.equals(fallbackName, ignoreCase = true) }?.id
            ?: allCategories.find { it.isExpense == isExpense }?.id
            ?: allCategories.firstOrNull { it.isDefault }?.id
            ?: allCategories.firstOrNull()?.id
            ?: defaultId
    }

    private fun matchHardcodedKeyword(input: String, isIncome: Boolean, allCategories: List<Category>): String? {
        val r = input.lowercase()
        val isExpense = !isIncome

        val categoryName = when {
            r.contains("kplc") || r.contains("tokens") || r.contains("power") || r.contains("jajemelo") ||
                    r.contains("water") || r.contains("sewerage") || r.contains("ncwsc") || r.contains("kiwasco") ||
                    r.contains("mawasco") || r.contains("nyewasco") || r.contains("eldowas") || r.contains("mowasco") ||
                    r.contains("gas") || r.contains("m-gas") || r.contains("mgas") || r.contains("afrigas") ||
                    r.contains("garbage") || r.contains("waste") || r.contains("trash") ||
                    r.contains("m-kopa") || r.contains("mkopa") || r.contains("d.light") || r.contains("sunking") || r.contains("bboxx") -> "Utilities"
            
            r.contains("zuku") || r.contains("safaricom home") || r.contains("poa internet") || r.contains("vilcom") ||
                    r.contains("faiba") || r.contains("jtl") || r.contains("jtlk") || r.contains("wananchi") ||
                    r.contains("mawingu") || r.contains("starlink") || r.contains("konnect") || r.contains("fibre connect") ||
                    r.contains("fiber connect") || r.contains("airtel fibre") || r.contains("telkom home") ||
                    r.contains("liquid home") || r.contains("liquid telecom") ||
                    r.contains("data bundles") || r.contains("data bundle") || r.contains("offers") || r.contains("tunukiwa") ||
                    r.contains("internet") || r.contains("bundles") -> "Internet"
            
            r.contains("airtime") || r.contains("tingg") || r.contains("top up") -> "Airtime"
            
            r.contains("supermarket") || r.contains("naivas") || r.contains("carrefour") || r.contains("quickmart") || r.contains("butchery") || r.contains("quick mart") || r.contains("friendly 5") || r.contains("slice city") || r.contains("memento butchery") -> "Groceries"
            
            r.contains("restaurant") || r.contains("cafe") || r.contains("kfc") || r.contains("java") || r.contains("lounge") || r.contains("chicken inn") || r.contains("pizza inn") || r.contains("creamy inn") || r.contains("choma place") || r.contains("nas n001") || r.contains("caterers") || r.contains("dishes") || r.contains("glovo") || r.contains("uber eats") || r.contains("bolt food") || r.contains("jumia food") || r.contains("eat") || r.contains("delivery") -> "Dining Out"
            
            r.contains("equity") || r.contains("co-operative") || r.contains("bank") || r.contains("i&m") || r.contains("ncba") || r.contains("boa") || r.contains("family bank") || r.contains("stanbic") || r.contains("loop") || r.contains("sidian") -> "Bank"
            
            r.contains("loan repayment") || r.contains("loan") || r.contains("fuliza") || r.contains("tala") || r.contains("branch") -> "Loans"
            
            r.contains("m-shwari saving") || r.contains("mshwari saving") || r.contains("m-shwari") || r.contains("mshwari") || r.contains("kcb") || r.contains("sacco") || r.contains("chama") || r.contains("orokise") ||
                    r.contains("zimele") || r.contains("etica") || r.contains("gulfcap") || r.contains("cytonn") || r.contains("arvocap") ||
                    r.contains("lofty") || r.contains("kuza") || r.contains("mali") || r.contains("ziidi") || r.contains("kasha") ||
                    r.contains("genghis") || r.contains("hela imara") || r.contains("nabo capital") ||
                    r.contains("stima sacco") || r.contains("police sacco") || r.contains("unaitas") || r.contains("mwalimu") ||
                    r.contains("harambee") || r.contains("kimisitu") || r.contains("hazina sacco") || r.contains("imarisha") ||
                    r.contains("tower sacco") || r.contains("waumini") ||
                    r.contains("dry associates") || r.contains("m-pesa saving") || r.contains("money market") || r.contains("fund") || r.contains("asset") || r.contains("mmf") -> "Savings"
            
            r.contains("tithe") || r.contains("offering") || r.contains("citam") || r.contains("church") || r.contains("charity") || r.contains("mosque") || r.contains("prayer mountain") -> "Charity"
            
            r.contains("parking") || r.contains("kaps") || r.contains("bolt") || r.contains("uber") || r.contains("taxi") || r.contains("rubis") || r.contains("totalenergies") || r.contains("shell") || r.contains("little") || r.contains("wasili") || r.contains("indriver") -> "Transport"
            
            r.contains("chemist") || r.contains("pharmacy") || r.contains("hospital") || r.contains("health") || r.contains("clinic") || r.contains("meds") || r.contains("dental") || r.contains("hopemed") || r.contains("medical") -> "Health"
            
            r.contains("netflix") || r.contains("spotify") || r.contains("showmax") || r.contains("youtube") || r.contains("prime") || r.contains("openai") || r.contains("chatgpt") -> "Subscriptions"
            
            r.contains("jumia") || r.contains("leather") || r.contains("watches") || r.contains("perfume") || r.contains("clothes") || r.contains("fashion") || r.contains("mrp") || r.contains("miniso") || r.contains("woolworths") || r.contains("tushop") || r.contains("m-pesa card") || r.contains("canva") || r.contains("pdfaid") -> "Shopping"
            
            r.contains("salon") || r.contains("barber") || r.contains("beauty") || r.contains("nail bar") -> "Personal Care"
            
            r.contains("hardware") || r.contains("timber") || r.contains("maintenance") || r.contains("repair") -> "Maintenance"
            
            r.contains("e-citizen") || r.contains("kra") || r.contains("county") -> "Government"
            
            r.contains("britam") || r.contains("nhif") || r.contains("shif") || r.contains("insurance") || r.contains("apa") ||
                    r.contains("jubilee") || r.contains("sanlam") || r.contains("cic") || r.contains("old mutual") ||
                    r.contains("icea lion") || r.contains("madison") || r.contains("apollo") || r.contains("ga insurance") ||
                    r.contains("heritage") || r.contains("geminia") || r.contains("pioneer") || r.contains("kenindia") || r.contains("uap") -> "Insurance"
            
            r.contains("salary") -> "Salary"
            r.contains("bonus") -> "Bonus"
            r.contains("interest") -> "Interest"
            r.contains("commission") || r.contains("income") -> "Other Income"
            
            else -> null
        }

        return if (categoryName != null) {
            allCategories.find { it.name.equals(categoryName, ignoreCase = true) && it.isExpense == isExpense }?.id
                ?: allCategories.find { it.name.equals(categoryName, ignoreCase = true) }?.id
        } else null
    }
}
