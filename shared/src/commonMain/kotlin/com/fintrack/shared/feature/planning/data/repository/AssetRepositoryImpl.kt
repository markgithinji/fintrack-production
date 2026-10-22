package com.fintrack.shared.feature.planning.data.repository

import com.fintrack.shared.db.FintrackDatabase
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.planning.domain.model.Asset
import com.fintrack.shared.feature.planning.domain.model.AssetType
import com.fintrack.shared.feature.planning.domain.repository.AssetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class AssetRepositoryImpl(
    private val database: FintrackDatabase
) : AssetRepository {

    override suspend fun getAssets(userId: String): Result<List<Asset>> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.createAssetTableIfNotExists()
            val entities = database.fintrackDatabaseQueries.selectAllAssets(userId).executeAsList()
            val assets = entities.map { entity ->
                Asset(
                    id = entity.id,
                    userId = entity.userId,
                    name = entity.name,
                    assetType = runCatching { AssetType.valueOf(entity.assetType) }.getOrDefault(AssetType.MMF_YIELD),
                    currentBalance = entity.currentBalance,
                    annualYieldPercentage = entity.annualYieldPercentage,
                    categoryId = entity.categoryId,
                    accountId = entity.accountId,
                    colorHex = entity.colorHex,
                    createdAt = entity.createdAt
                )
            }
            Result.Success(assets)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getAssetById(id: String): Result<Asset?> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.createAssetTableIfNotExists()
            val entity = database.fintrackDatabaseQueries.selectAssetById(id).executeAsOneOrNull()
            val asset = entity?.let {
                Asset(
                    id = it.id,
                    userId = it.userId,
                    name = it.name,
                    assetType = runCatching { AssetType.valueOf(it.assetType) }.getOrDefault(AssetType.MMF_YIELD),
                    currentBalance = it.currentBalance,
                    annualYieldPercentage = it.annualYieldPercentage,
                    categoryId = it.categoryId,
                    accountId = it.accountId,
                    colorHex = it.colorHex,
                    createdAt = it.createdAt
                )
            }
            Result.Success(asset)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun saveAsset(asset: Asset): Result<Asset> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.createAssetTableIfNotExists()
            database.fintrackDatabaseQueries.insertAsset(
                id = asset.id,
                userId = asset.userId,
                name = asset.name,
                assetType = asset.assetType.name,
                currentBalance = asset.currentBalance,
                annualYieldPercentage = asset.annualYieldPercentage,
                categoryId = asset.categoryId,
                accountId = asset.accountId,
                colorHex = asset.colorHex,
                createdAt = asset.createdAt
            )
            Result.Success(asset)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateAssetBalance(id: String, newBalance: Double): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.createAssetTableIfNotExists()
            database.fintrackDatabaseQueries.updateAssetBalance(newBalance, id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteAsset(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.fintrackDatabaseQueries.createAssetTableIfNotExists()
            database.fintrackDatabaseQueries.deleteAsset(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
