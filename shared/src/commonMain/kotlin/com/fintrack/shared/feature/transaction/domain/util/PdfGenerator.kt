package com.fintrack.shared.feature.transaction.domain.util

import com.fintrack.shared.feature.transaction.domain.model.Transaction

expect fun generatePdfBytes(transactions: List<Transaction>): ByteArray
