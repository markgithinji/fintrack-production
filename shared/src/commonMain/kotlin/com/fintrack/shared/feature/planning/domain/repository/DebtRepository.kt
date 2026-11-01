package com.fintrack.shared.feature.planning.domain.repository

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.planning.domain.model.Debt

interface DebtRepository {
    suspend fun getDebts(userId: String = "local_user"): Result<List<Debt>>
    suspend fun getDebtById(id: String): Result<Debt?>
    suspend fun saveDebt(debt: Debt): Result<Debt>
    suspend fun updateDebtBalance(id: String, newBalance: Double): Result<Unit>
    suspend fun deleteDebt(id: String): Result<Unit>
}
