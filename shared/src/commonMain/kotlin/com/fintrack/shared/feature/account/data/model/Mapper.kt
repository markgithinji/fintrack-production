package com.fintrack.shared.feature.account.data.model

import com.fintrack.shared.feature.account.domain.model.Account
import com.fintrack.shared.feature.account.domain.model.AccountType

fun Account.toCreateRequest(): CreateAccountRequest = CreateAccountRequest(
    name = this.name,
    type = this.type.toDto(),
    balance = this.balance,
    linkedSources = this.linkedSources,
    isDefault = this.isDefault
)

fun Account.toUpdateRequest(): UpdateAccountRequest = UpdateAccountRequest(
    name = this.name,
    type = this.type.toDto(),
    balance = this.balance,
    lastSyncedAt = this.lastSyncedAt,
    linkedSources = this.linkedSources,
    isDefault = this.isDefault
)

fun AccountDto.toDomain(): Account = Account(
    id = this.id ?: "",   // Empty string for new inserts
    name = this.name,
    balance = this.balance,
    income = this.income,
    expense = this.expense,
    isDefault = this.isDefault ?: false,
    type = this.type?.toDomain() ?: AccountType.OTHER,
    linkedSources = this.linkedSources ?: emptyList(),
    createdAt = this.createdAt,
    lastSyncedAt = this.lastSyncedAt
)

fun AccountType.toDto(): AccountTypeDto = when (this) {
    AccountType.OTHER -> AccountTypeDto.OTHER
    AccountType.MPESA -> AccountTypeDto.MPESA
    AccountType.BANK -> AccountTypeDto.BANK
    AccountType.CASH -> AccountTypeDto.CASH
    AccountType.WALLET -> AccountTypeDto.WALLET
    AccountType.SAVINGS -> AccountTypeDto.SAVINGS
}

fun AccountTypeDto.toDomain(): AccountType = when (this) {
    AccountTypeDto.OTHER -> AccountType.OTHER
    AccountTypeDto.MPESA -> AccountType.MPESA
    AccountTypeDto.BANK -> AccountType.BANK
    AccountTypeDto.CASH -> AccountType.CASH
    AccountTypeDto.WALLET -> AccountType.WALLET
    AccountTypeDto.SAVINGS -> AccountType.SAVINGS
}
