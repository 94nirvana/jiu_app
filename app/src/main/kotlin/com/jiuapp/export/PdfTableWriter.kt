package com.jiuapp.export

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

/**
 * Gera um PDF simples de tabela (cabeçalho + linhas) com o PdfDocument do framework.
 * Sem dependências externas: o conteúdo é texto puro, sem HTML, então não há injeção de markup.
 */
object PdfTableWriter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 18f
    private const val TITLE_SIZE = 16f
    private const val BODY_SIZE = 10f
    private const val ROWS_PER_PAGE = 40

    fun build(title: String, header: List<String>, rows: List<List<String>>): ByteArray {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = TITLE_SIZE; isFakeBoldText = true; isAntiAlias = true }
        val bodyPaint = Paint().apply { textSize = BODY_SIZE; isAntiAlias = true }
        val pages = rows.chunked(ROWS_PER_PAGE).ifEmpty { listOf(emptyList()) }
        pages.forEachIndexed { pageIndex, pageRows ->
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
            val page = document.startPage(info)
            val canvas = page.canvas
            var y = MARGIN
            canvas.drawText(title, MARGIN, y, titlePaint)
            y += LINE_HEIGHT * 2
            val columnWidth = (PAGE_WIDTH - 2 * MARGIN) / header.size.coerceAtLeast(1)
            header.forEachIndexed { index, cell ->
                canvas.drawText(cell.take(MAX_CELL_CHARS), MARGIN + index * columnWidth, y, titlePaint)
            }
            y += LINE_HEIGHT
            pageRows.forEach { row ->
                row.forEachIndexed { index, cell ->
                    canvas.drawText(cell.take(MAX_CELL_CHARS), MARGIN + index * columnWidth, y, bodyPaint)
                }
                y += LINE_HEIGHT
            }
            document.finishPage(page)
        }
        return ByteArrayOutputStream().use { out ->
            document.writeTo(out)
            document.close()
            out.toByteArray()
        }
    }

    private const val MAX_CELL_CHARS = 28
}
