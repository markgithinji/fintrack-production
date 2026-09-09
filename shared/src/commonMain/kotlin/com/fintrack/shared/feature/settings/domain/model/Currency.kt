package com.fintrack.shared.feature.settings.domain.model

enum class Currency(val code: String, val symbol: String) {
    KES("KES", "KSh"),
    USD("USD", "$"),
    EUR("EUR", "€"),
    GBP("GBP", "£"),
    UGX("UGX", "USh"),
    TZS("TZS", "TSh");

    companion object {
        fun fromCode(code: String?): Currency {
            return entries.find { it.code == code } ?: KES
        }
    }
}