package com.fintrack.shared.feature.core.ui.util

import androidx.compose.runtime.Composable

/**
 * Creates and remembers a platform-specific image picker launcher.
 * Returns a lambda that can be called to launch the image picker.
 */
@Composable
expect fun rememberImagePickerLauncher(onImagePicked: (ByteArray?) -> Unit): () -> Unit
