package com.fintrack.shared.feature.core.util

import java.util.UUID

actual fun randomUUID(): String = UUID.randomUUID().toString()
