package com.fintrack.shared.feature.planning.data.repository

import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.planning.domain.model.Debt
import com.fintrack.shared.feature.planning.domain.repository.DebtRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class DebtRepositoryImpl(
    private val database: FintrackDatabase
) : DebtRepository {

    override suspend fun getDebts(userId: String): Result<List<Debt>> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.createDebtTableIfNotExists()
            val entities = database.fintrackDatabaseQueries.selectAllDebts(userId).executeAsList()
            val debts = entities.map { entity ->
                Debt(
                    id = entity.id,
                    userId = entity.userId,
                    name = entity.name,
                    balance = entity.balance,
                    initialBalance = entity.initialBalance,
                    interestRate = entity.interestRate,
                    minimumPayment = entity.minimumPayment,
                    categoryId = entity.categoryId,
                    accountId = entity.accountId,
                    colorHex = entity.colorHex,
                    createdAt = entity.createdAt
                )
            }
            Result.Success(debts)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getDebtById(id: String): Result<Debt?> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.createDebtTableIfNotExists()
            val entity = database.fintrackDatabaseQueries.selectDebtById(id).executeAsOneOrNull()
            val debt = entity?.let {
                Debt(
                    id = it.id,
                    userId = it.userId,
                    name = it.name,
                    balance = it.balance,
                    initialBalance = it.initialBalance,
                    interestRate = it.interestRate,
                    minimumPayment = it.minimumPayment,
                    categoryId = it.categoryId,
                    accountId = it.accountId,
                    colorHex = it.colorHex,
                    createdAt = it.createdAt
                )
            }
            Result.Success(debt)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun saveDebt(debt: Debt): Result<Debt> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.createDebtTableIfNotExists()
            database.fintrackDatabaseQueries.insertDebt(
                id = debt.id,
                userId = debt.userId,
                name = debt.name,
                balance = debt.balance,
                initialBalance = debt.initialBalance,
                interestRate = debt.interestRate,
                minimumPayment = debt.minimumPayment,
                categoryId = debt.categoryId,
                accountId = debt.accountId,
                colorHex = debt.colorHex,
                createdAt = debt.createdAt
            )
            Result.Success(debt)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateDebtBalance(id: String, newBalance: Double): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.createDebtTableIfNotExists()
            database.fintrackDatabaseQueries.updateDebtBalance(newBalance, id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteDebt(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.createDebtTableIfNotExists()
            database.fintrackDatabaseQueries.deleteDebt(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
