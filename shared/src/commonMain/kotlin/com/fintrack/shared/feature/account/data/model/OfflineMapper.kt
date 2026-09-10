package com.fintrack.shared.feature.account.data.model

import com.fintrack.shared.db.AccountEntity
import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType
import com.ionspin.kotlin.bignum.decimal.BigDecimal

fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        name = name,
        balance = BigDecimal.parseString(balance),
        isDefault = isDefault != 0L,
        type = try { AccountType.valueOf(type) } catch(e: Exception) { AccountType.OTHER },
        linkedSources = linkedSources,
        createdAt = createdAt,
        lastSyncedAt = lastSyncedAt
    )
}
