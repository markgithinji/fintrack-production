package com.fintrack.shared.feature.transaction.domain.util

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.fintrack.shared.feature.transaction.domain.model.Transaction
import java.io.ByteArrayOutputStream

actual fun generatePdfBytes(transactions: List<Transaction>): ByteArray {
    val pdfDocument = PdfDocument()
    val titlePaint = Paint().apply {
        textSize = 18f
        isFakeBoldText = true
    }
    val bodyPaint = Paint().apply {
        textSize = 12f
    }

    var pageNumber = 1
    var yPos = 80f
    val margin = 50f
    val pageWidth = 595 // A4
    val pageHeight = 842 // A4
    val lineSpacing = 20f

    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    canvas.drawText("FINTRACK TRANSACTION REPORT", margin, 40f, titlePaint)
    canvas.drawText("Date | Category | Amount", margin, 65f, titlePaint)

    transactions.forEach { transaction ->
        if (yPos > pageHeight - margin) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPos = margin
        }

        val text = "${transaction.dateTime} | ${transaction.category} | ${transaction.totalAmount}"
        canvas.drawText(text, margin, yPos, bodyPaint)
        yPos += lineSpacing
    }

    pdfDocument.finishPage(page)

    val outputStream = ByteArrayOutputStream()
    pdfDocument.writeTo(outputStream)
    pdfDocument.close()
    return outputStream.toByteArray()
}
