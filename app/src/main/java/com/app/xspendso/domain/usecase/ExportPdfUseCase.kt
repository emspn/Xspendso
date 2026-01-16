package com.app.xspendso.domain.usecase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.app.xspendso.data.TransactionEntity
import java.io.File
import java.io.FileOutputStream

class ExportPdfUseCase(private val context: Context) {
    operator fun invoke(transactions: List<TransactionEntity>, fileName: String): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        var y = 40f
        canvas.drawText("Xpendso Transaction Report", 40f, y, paint)
        y += 40f

        transactions.forEach {
            val text = "${it.counterparty} (${it.accountSource}): ₹${Math.abs(it.amount)} [${it.category}]"
            canvas.drawText(text, 40f, y, paint)
            y += 20f
            if (y > 800) return@forEach // Basic pagination limit
        }

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(null), fileName)
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}
