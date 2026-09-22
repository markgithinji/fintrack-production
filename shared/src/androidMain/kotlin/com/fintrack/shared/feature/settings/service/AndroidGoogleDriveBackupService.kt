package com.fintrack.shared.feature.settings.service

import android.content.Context
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.Result
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AndroidGoogleDriveBackupService(
    private val context: Context,
    private val logger: KMPLogger
) : CloudDriveBackupService {

    private val tag = "GoogleDriveBackup"
    private val backupFileName = "fintrack_google_drive_backup.json"

    private suspend fun getDriveService(): Result<Drive> = withContext(Dispatchers.IO) {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                .build()

            val googleSignInClient = GoogleSignIn.getClient(context, gso)

            // Try silent sign-in first
            val account: GoogleSignInAccount = try {
                val task = googleSignInClient.silentSignIn()
                task.await()
            } catch (e: Exception) {
                // If silent sign in fails, the user needs to sign in explicitly.
                // In a production app with a full UI flow, this would launch an intent.
                // Here we attempt to get the last signed-in account if one exists.
                val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
                if (lastAccount != null && GoogleSignIn.hasPermissions(lastAccount, Scope(DriveScopes.DRIVE_APPDATA))) {
                    lastAccount
                } else {
                    return@withContext Result.Error(Exception("User not signed in or permissions not granted for Google Drive AppData. Please sign in via the app.", e))
                }
            }

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccount = account.account

            val drive = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("FinTrack")
                .build()

            Result.Success(drive)
        } catch (e: Exception) {
            logger.error(tag, "Failed to initialize Drive service", e)
            Result.Error(e)
        }
    }

    override suspend fun uploadBackup(jsonContent: String): Result<String> = withContext(Dispatchers.IO) {
        val driveResult = getDriveService()
        if (driveResult is Result.Error) {
            return@withContext driveResult
        }
        val driveService = (driveResult as Result.Success).data

        try {
            // Check if file already exists in AppData folder
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("nextPageToken, files(id, name)")
                .setQ("name = '$backupFileName'")
                .execute()

            val existingFile = fileList.files?.firstOrNull()

            val content = ByteArrayContent.fromString("application/json", jsonContent)

            if (existingFile != null) {
                // Update existing file
                driveService.files().update(existingFile.id, null, content).execute()
                logger.info(tag, "Google Drive AppData backup file updated successfully")
            } else {
                // Create new file
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = backupFileName
                    parents = listOf("appDataFolder")
                }
                driveService.files().create(fileMetadata, content).execute()
                logger.info(tag, "Google Drive AppData backup file created successfully")
            }

            Result.Success(backupFileName)
        } catch (e: Exception) {
            logger.error(tag, "Failed to upload Google Drive AppData backup", e)
            Result.Error(e)
        }
    }

    override suspend fun downloadLatestBackup(): Result<String> = withContext(Dispatchers.IO) {
        val driveResult = getDriveService()
        if (driveResult is Result.Error) {
            return@withContext driveResult
        }
        val driveService = (driveResult as Result.Success).data

        try {
            // Find the file in AppData folder
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("nextPageToken, files(id, name)")
                .setQ("name = '$backupFileName'")
                .execute()

            val existingFile = fileList.files?.firstOrNull()
                ?: return@withContext Result.Error(Exception("No Google Drive backup file found in AppData"))

            // Download file content
            val outputStream = java.io.ByteArrayOutputStream()
            driveService.files().get(existingFile.id).executeMediaAndDownloadTo(outputStream)
            
            val jsonContent = outputStream.toString("UTF-8")
            logger.info(tag, "Google Drive AppData backup file downloaded successfully")
            Result.Success(jsonContent)
        } catch (e: Exception) {
            logger.error(tag, "Failed to download Google Drive backup", e)
            Result.Error(e)
        }
    }
}
