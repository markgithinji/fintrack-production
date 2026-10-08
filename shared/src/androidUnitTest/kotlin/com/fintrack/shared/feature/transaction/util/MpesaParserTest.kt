package com.fintrack.shared.feature.transaction.util

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MpesaParserTest {

    @Test
    fun `parse sent money SMS extracts details correctly`() {
        val sms = "QZE8901234 Confirmed. Ksh1,500.00 sent to JOHN DOE 0712345678 on 15/1/25 at 2:30 PM. Transaction cost Ksh25.00. New M-PESA balance is Ksh10,000.00."
        val transaction = MpesaParser.parse(sms)

        assertNotNull(transaction)
        assertEquals("QZE8901234", transaction.externalId)
        assertFalse(transaction.isIncome)
        assertEquals(BigDecimal.parseString("1500.00"), transaction.amount)
        assertEquals(BigDecimal.parseString("25.00"), transaction.transactionCost)
        assertEquals(BigDecimal.parseString("10000.00"), transaction.balance)
    }

    @Test
    fun `parse received money SMS extracts income details`() {
        val sms = "QZE8901235 Confirmed. You have received Ksh5,000.00 from MARY WANJIKU 0722001122 on 15/1/25 at 3:15 PM. New M-PESA balance is Ksh15,000.00."
        val transaction = MpesaParser.parse(sms)

        assertNotNull(transaction)
        assertEquals("QZE8901235", transaction.externalId)
        assertTrue(transaction.isIncome)
        assertEquals(BigDecimal.parseString("5000.00"), transaction.amount)
        assertEquals(BigDecimal.ZERO, transaction.transactionCost)
        assertEquals(BigDecimal.parseString("15000.00"), transaction.balance)
    }

    @Test
    fun `parse paybill payment SMS extracts merchant name`() {
        val sms = "QZE8901236 Confirmed. Ksh3,200.00 paid to JAVA HOUSE WESTLANDS on 15/1/25 at 8:00 PM. Transaction cost Ksh0.00."
        val transaction = MpesaParser.parse(sms)

        assertNotNull(transaction)
        assertEquals("QZE8901236", transaction.externalId)
        assertFalse(transaction.isIncome)
        assertEquals(BigDecimal.parseString("3200.00"), transaction.amount)
    }

    @Test
    fun `parse non-transactional balance notification returns null`() {
        val sms = "Fuliza M-Pesa amount is Ksh 0.00. Your loan limit is Ksh 5,000.00."
        val transaction = MpesaParser.parse(sms)

        assertNull(transaction)
    }

    @Test
    fun `parse unconfirmed SMS returns null`() {
        val sms = "Your transaction failed due to insufficient funds."
        val transaction = MpesaParser.parse(sms)

        assertNull(transaction)
    }
}
