package com.app.xspendso.domain.usecase

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.app.xspendso.data.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class ExportReportUseCase(private val context: Context) {
    
    fun exportToPdf(transactions: List<TransactionEntity>, fileName: String): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // Header Styling
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        paint.color = Color.BLACK
        canvas.drawText("Xpendso Financial Report", 40f, 60f, paint)
        
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.GRAY
        canvas.drawText("Generated on: ${dateFormat.format(Date())}", 40f, 80f, paint)

        // Summary Section
        val totalSpent = transactions.filter { it.type == "DEBIT" }.sumOf { abs(it.amount) }
        val totalIncome = transactions.filter { it.type == "CREDIT" }.sumOf { abs(it.amount) }
        
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Summary:", 40f, 120f, paint)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Total Spent: ₹${String.format(Locale.getDefault(), "%.2f", totalSpent)}", 40f, 140f, paint)
        canvas.drawText("Total Income: ₹${String.format(Locale.getDefault(), "%.2f", totalIncome)}", 40f, 160f, paint)

        // Table Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var y = 200f
        canvas.drawText("Date", 40f, y, paint)
        canvas.drawText("Counterparty", 120f, y, paint)
        canvas.drawText("Category", 350f, y, paint)
        canvas.drawText("Amount", 500f, y, paint)
        
        canvas.drawLine(40f, y + 5, 550f, y + 5, paint)
        y += 25f

        // Transactions List
        paint.typeface = Typeface.DEFAULT
        transactions.sortedByDescending { it.timestamp }.forEach { tx ->
            if (y > 800) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 60f
            }

            canvas.drawText(dateFormat.format(Date(tx.timestamp)), 40f, y, paint)
            val name = if (tx.counterparty.length > 25) tx.counterparty.take(22) + "..." else tx.counterparty
            canvas.drawText(name, 120f, y, paint)
            canvas.drawText(tx.category, 350f, y, paint)
            
            paint.color = if (tx.type == "DEBIT") Color.RED else Color.parseColor("#10B981")
            val amountText = if (tx.type == "DEBIT") "-₹${abs(tx.amount)}" else "+₹${abs(tx.amount)}"
            canvas.drawText(amountText, 500f, y, paint)
            
            paint.color = Color.BLACK
            y += 20f
        }

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(null), fileName)
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun exportToCsv(transactions: List<TransactionEntity>, fileName: String): File? {
        val file = File(context.getExternalFilesDir(null), fileName)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        return try {
            FileOutputStream(file).use { out ->
                val header = "ID,Date,AccountSource,Counterparty,Category,Amount,Type,Method,Remark\n"
                out.write(header.toByteArray())
                
                transactions.forEach { tx ->
                    val line = "${tx.id},${dateFormat.format(Date(tx.timestamp))},${tx.accountSource},${tx.counterparty},${tx.category},${tx.amount},${tx.type},${tx.method},${tx.remark ?: ""}\n"
                    out.write(line.toByteArray())
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
