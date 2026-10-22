package com.fintrack.shared.feature.planning.domain.repository

import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.planning.domain.model.Asset

interface AssetRepository {
    suspend fun getAssets(userId: String = "local_user"): Result<List<Asset>>
    suspend fun getAssetById(id: String): Result<Asset?>
    suspend fun saveAsset(asset: Asset): Result<Asset>
    suspend fun updateAssetBalance(id: String, newBalance: Double): Result<Unit>
    suspend fun deleteAsset(id: String): Result<Unit>
}
