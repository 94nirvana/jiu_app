package com.jiuapp.ui.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiuapp.MainActivity
import com.jiuapp.R
import com.jiuapp.export.ShareHelper
import com.jiuapp.ui.common.SectionCard
import com.jiuapp.ui.common.Spacing
import com.jiuapp.ui.techniques.BackButton
import java.time.LocalDate
import kotlin.system.exitProcess
import kotlinx.coroutines.launch

private const val BACKUP_MIME = "application/json"
private const val CSV_MIME = "text/csv"
private const val PDF_MIME = "application/pdf"
private const val DELETE_CONFIRM_WORD = "APAGAR"

@Composable
fun DataScreen(onBack: () -> Unit, viewModel: DataViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pdfTitle = stringResource(R.string.data_pdf_title)
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }

    val messageText = state.message?.let { stringResource(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) restartApp(context)
    }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.backupJson()?.let { json ->
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
            }
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            if (json != null) viewModel.restore(json)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.data_title)) }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionCard(title = stringResource(R.string.data_backup_title)) {
                Text(stringResource(R.string.data_backup_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                OutlinedButton(
                    onClick = { createBackup.launch("jiuapp-backup-${LocalDate.now()}.json") },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
                ) { Text(stringResource(R.string.data_export_backup)) }
                OutlinedButton(
                    onClick = { openBackup.launch(arrayOf(BACKUP_MIME)) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
                ) { Text(stringResource(R.string.data_import_backup)) }
            }

            SectionCard(title = stringResource(R.string.data_export_title)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val csv = viewModel.sessionsCsv()
                            share(context, "treinos.csv", csv.toByteArray(Charsets.UTF_8), CSV_MIME)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
                ) { Text(stringResource(R.string.data_csv_sessions)) }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val csv = viewModel.injuriesCsv()
                            share(context, "lesoes.csv", csv.toByteArray(Charsets.UTF_8), CSV_MIME)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
                ) { Text(stringResource(R.string.data_csv_injuries)) }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val csv = viewModel.weightsCsv()
                            share(context, "peso.csv", csv.toByteArray(Charsets.UTF_8), CSV_MIME)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
                ) { Text(stringResource(R.string.data_csv_weights)) }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val pdf = viewModel.sessionsPdf(pdfTitle)
                            share(context, "treinos.pdf", pdf, PDF_MIME)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
                ) { Text(stringResource(R.string.data_pdf_sessions)) }
            }

            SectionCard(title = stringResource(R.string.data_delete_title)) {
                Text(stringResource(R.string.data_delete_body), style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = { confirmDelete = true },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
                ) { Text(stringResource(R.string.data_delete_action)) }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.data_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(stringResource(R.string.data_delete_confirm_prompt, DELETE_CONFIRM_WORD))
                    OutlinedTextField(typed, { typed = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.deleteAll()
                    },
                    enabled = typed == DELETE_CONFIRM_WORD,
                ) { Text(stringResource(R.string.data_delete_action)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun share(context: Context, name: String, bytes: ByteArray, mime: String) {
    val file = ShareHelper.writeFile(context, name, bytes)
    context.startActivity(Intent.createChooser(ShareHelper.shareIntent(context, file, mime), null))
}

/** Após apagar tudo, reinicia o processo: o banco e as chaves foram removidos, e o Room não reabre o arquivo antigo. */
private fun restartApp(context: Context) {
    val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    exitProcess(0)
}
