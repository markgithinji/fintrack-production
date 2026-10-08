package com.fintrack.shared.feature.transaction.domain.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReceiptParserTest {

    @Test
    fun `parseAmount extracts max amount from receipt text`() {
        val receiptText = """
            NAIVAS SUPERMARKET
            Items:
            Bread: 120.00
            Milk: 110.00
            Subtotal: 230.00
            VAT (16%): 36.80
            TOTAL: 266.80
            THANK YOU FOR SHOPPING
        """.trimIndent()

        val amount = ReceiptParser.parseAmount(receiptText)
        assertEquals(266.80, amount)
    }

    @Test
    fun `parseAmount returns null when no amount found`() {
        val receiptText = "THANK YOU FOR SHOPPING WITH US"
        assertNull(ReceiptParser.parseAmount(receiptText))
    }

    @Test
    fun `parseDate extracts YYYY-MM-DD date correctly`() {
        val receiptText = """
            JAVA HOUSE
            Date: 2025-01-15 14:30
            Total: 1250.00
        """.trimIndent()

        val date = ReceiptParser.parseDate(receiptText)
        assertEquals(LocalDate(2025, 1, 15), date)
    }

    @Test
    fun `parseDate extracts DD-MM-YYYY date correctly`() {
        val receiptText = """
            CARREFOUR
            Date: 25/12/2024
            Total: 4500.00
        """.trimIndent()

        val date = ReceiptParser.parseDate(receiptText)
        assertEquals(LocalDate(2024, 12, 25), date)
    }

    @Test
    fun `parseMerchant extracts prominent merchant name`() {
        val receiptText = """
            KFC Kenya Ltd
            Branch: Westlands
            Date: 2025-02-01
        """.trimIndent()

        val merchant = ReceiptParser.parseMerchant(receiptText)
        assertEquals("KFC Kenya Ltd", merchant)
    }

    @Test
    fun `isMpesa detects safaricom or mpesa keywords`() {
        assertTrue(ReceiptParser.isMpesa("Confirmed. M-PESA Paybill 522522"))
        assertTrue(ReceiptParser.isMpesa("Safaricom Lipa Na M-Pesa"))
        assertFalse(ReceiptParser.isMpesa("Standard Chartered Bank Receipt"))
    }
}
