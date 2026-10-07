package com.fintrack.shared.feature.settings.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsModelsTest {

    @Test
    fun `AppTheme fromName parses correctly or defaults to SYSTEM`() {
        assertEquals(AppTheme.DARK, AppTheme.fromName("DARK"))
        assertEquals(AppTheme.SYSTEM, AppTheme.fromName("INVALID"))
        assertEquals(AppTheme.SYSTEM, AppTheme.fromName(null))
    }

    @Test
    fun `Currency fromCode parses correctly or defaults to KES`() {
        assertEquals(Currency.USD, Currency.fromCode("USD"))
        assertEquals(Currency.KES, Currency.fromCode("INVALID"))
        assertEquals(Currency.KES, Currency.fromCode(null))
    }

    @Test
    fun `TimeFormat fromName parses correctly or defaults to TWENTY_FOUR_HOUR`() {
        assertEquals(TimeFormat.TWELVE_HOUR, TimeFormat.fromName("TWELVE_HOUR"))
        assertEquals(TimeFormat.TWENTY_FOUR_HOUR, TimeFormat.fromName(null))
    }
}
