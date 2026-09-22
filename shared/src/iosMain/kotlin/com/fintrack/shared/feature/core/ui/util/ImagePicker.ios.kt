package com.fintrack.shared.feature.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (ByteArray?) -> Unit): () -> Unit {
    // Stub for iOS. Implementing a full UIImagePickerController requires specific platform code.
    return remember { { onImagePicked(null) } }
}
