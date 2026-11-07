package com.jiuapp.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Compartilhamento de arquivos gerados (CSV, PDF). Os arquivos ficam no cache do app e são
 * entregues por FileProvider com permissão de leitura temporária, sem expor o diretório inteiro.
 */
object ShareHelper {

    private const val EXPORT_DIR = "exports"

    fun writeFile(context: Context, fileName: String, bytes: ByteArray): File {
        require(fileName.matches(SAFE_NAME)) { "Nome de arquivo inválido" }
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        return File(dir, fileName).apply { writeBytes(bytes) }
    }

    fun shareIntent(context: Context, file: File, mimeType: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.exports", file)
        return Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private val SAFE_NAME = Regex("^[A-Za-z0-9._-]{1,80}$")
}
