package com.fintrack.shared.feature.core.util

import platform.Foundation.NSUUID

actual fun randomUUID(): String = NSUUID().UUIDString()
