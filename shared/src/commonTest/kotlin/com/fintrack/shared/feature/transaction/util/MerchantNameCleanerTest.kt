package com.fintrack.shared.feature.transaction.util

import kotlin.test.Test
import kotlin.test.assertEquals

class MerchantNameCleanerTest {

    @Test
    fun `cleans masked phone numbers with asterisks for income payers`() {
        val result1 = MerchantNameCleaner.clean("Received from JOHN DOE 07****1234")
        assertEquals("John DOE", result1)

        val result2 = MerchantNameCleaner.clean("You have received Ksh 5,000 from MARY JANE 2547****5678")
        assertEquals("Mary Jane", result2)

        val result3 = MerchantNameCleaner.clean("Paid to NAIVAS SUPERMARKET 2547****9012")
        assertEquals("Naivas Supermarket", result3)
    }
}
