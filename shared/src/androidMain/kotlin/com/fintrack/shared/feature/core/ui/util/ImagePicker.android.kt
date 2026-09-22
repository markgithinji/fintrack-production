package com.fintrack.shared.feature.core.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

tailrec fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (ByteArray?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val activity = context.getActivity()
    
    // Configure ML Kit Document Scanner
    // This gives us a native camera UI with auto-cropping AND a gallery import button!
    val scannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true) // Allows picking from gallery inside the camera UI
        .setPageLimit(1)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
        .build()
        
    val scanner = GmsDocumentScanning.getClient(scannerOptions)
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uri = scanResult?.pages?.firstOrNull()?.imageUri
            if (uri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    onImagePicked(bytes)
                } catch (e: Exception) {
                    e.printStackTrace()
                    onImagePicked(null)
                }
            } else {
                onImagePicked(null)
            }
        } else {
            onImagePicked(null) // Cancelled or failed
        }
    }

    return remember { 
        { 
            activity?.let { act ->
                scanner.getStartScanIntent(act)
                    .addOnSuccessListener { intentSender ->
                        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener {
                        onImagePicked(null)
                    }
            }
        } 
    }
}

