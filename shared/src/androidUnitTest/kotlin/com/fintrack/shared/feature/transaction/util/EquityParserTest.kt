package com.fintrack.shared.feature.transaction.util

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EquityParserTest {

    @Test
    fun `parse equity payment to merchant SMS extracts details correctly`() {
        val sms = "Confirmed. Payment of KES 1,500.00 to JAVA HOUSE on 14/10/2026 14:30. Ref: EQ12345. Available Balance KES 45,000.00."
        val transaction = EquityParser.parse(sms)

        assertNotNull(transaction)
        assertEquals("EQ12345", transaction.externalId)
        assertFalse(transaction.isIncome)
        assertEquals(BigDecimal.parseString("1500.00"), transaction.amount)
        assertEquals(BigDecimal.parseString("45000.00"), transaction.balance)
    }

    @Test
    fun `parse equity debited for payment SMS extracts merchant and amount`() {
        val sms = "Dear Customer, KES 2,500.00 has been debited from account 0123... for payment to NAIVAS SUPERMARKET on 14-Oct-2026 at 10:15. Ref: EQ987654. Avail Bal: KES 30,000.00."
        val transaction = EquityParser.parse(sms)

        assertNotNull(transaction)
        assertEquals("EQ987654", transaction.externalId)
        assertFalse(transaction.isIncome)
        assertEquals(BigDecimal.parseString("2500.00"), transaction.amount)
        assertEquals(BigDecimal.parseString("30000.00"), transaction.balance)
    }

    @Test
    fun `parse equity received transfer SMS extracts income details`() {
        val sms = "Your account 0123... has been credited with KES 10,000.00 from MARY JANE on 14-Oct-2026. Ref: EQ789012. Avail Bal: KES 95,000.00."
        val transaction = EquityParser.parse(sms)

        assertNotNull(transaction)
        assertEquals("EQ789012", transaction.externalId)
        assertTrue(transaction.isIncome)
        assertEquals(BigDecimal.parseString("10000.00"), transaction.amount)
        assertEquals(BigDecimal.parseString("95000.00"), transaction.balance)
    }

    @Test
    fun `parse equity card auth SMS extracts merchant and ref`() {
        val sms = "CONFIRMED. KES 350.00, Auth for card 4000****1234 at JAVA HOUSE on 2026-10-14 12:00:00 Ref: EQ554433."
        val transaction = EquityParser.parse(sms)

        assertNotNull(transaction)
        assertEquals("EQ554433", transaction.externalId)
        assertFalse(transaction.isIncome)
        assertEquals(BigDecimal.parseString("350.00"), transaction.amount)
    }
}
