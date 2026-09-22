package com.fintrack.shared.feature.transaction.service

import android.graphics.BitmapFactory
import com.fintrack.shared.feature.core.logger.KMPLogger
import com.fintrack.shared.feature.core.util.Result
import com.fintrack.shared.feature.transaction.domain.service.ReceiptScanner
import com.fintrack.shared.feature.transaction.domain.service.ScannedReceiptData
import com.fintrack.shared.feature.transaction.domain.util.ReceiptParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AndroidReceiptScanner(
    private val logger: KMPLogger
) : ReceiptScanner {

    private val tag = "AndroidReceiptScanner"
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun scanReceipt(imageBytes: ByteArray): Result<ScannedReceiptData> = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return@withContext Result.Error(Exception("Failed to decode image bytes to Bitmap"))

            val image = InputImage.fromBitmap(bitmap, 0)
            
            val visionText = try {
                recognizer.process(image).await()
            } catch (e: Exception) {
                logger.error(tag, "ML Kit Text Recognition failed", e)
                return@withContext Result.Error(Exception("ML Kit Text Recognition failed: ${e.message}", e))
            }

            val rawText = visionText.text
            logger.info(tag, "Extracted text from receipt:\n$rawText")

            val amount = ReceiptParser.parseAmount(rawText)
            val date = ReceiptParser.parseDate(rawText)
            val merchant = ReceiptParser.parseMerchant(rawText)
            val isMpesa = ReceiptParser.isMpesa(rawText)
            val suggestedCategory = ReceiptParser.guessCategoryName(rawText, merchant)

            Result.Success(
                ScannedReceiptData(
                    amount = amount,
                    date = date,
                    merchantName = merchant,
                    isMpesa = isMpesa,
                    suggestedCategoryName = suggestedCategory,
                    rawText = rawText
                )
            )
        } catch (e: Exception) {
            logger.error(tag, "Failed to scan receipt", e)
            Result.Error(e)
        }
    }
}
