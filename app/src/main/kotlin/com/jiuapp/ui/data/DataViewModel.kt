package com.jiuapp.ui.data

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiuapp.R
import com.jiuapp.domain.DomainException
import com.jiuapp.domain.export.CsvExport
import com.jiuapp.domain.repository.DataRepository
import com.jiuapp.domain.repository.HealthRepository
import com.jiuapp.domain.repository.SessionRepository
import com.jiuapp.export.PdfTableWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DataUiState(
    val busy: Boolean = false,
    @StringRes val message: Int? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class DataViewModel @Inject constructor(
    private val data: DataRepository,
    private val sessions: SessionRepository,
    private val health: HealthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DataUiState())
    val state: StateFlow<DataUiState> = _state

    /** Retorna o conteúdo do backup, ou null em caso de falha (a mensagem vai para o estado). */
    suspend fun backupJson(): String? = guarded(R.string.data_export_failed) { data.exportBackup() }

    fun restore(json: String) = viewModelScope.launch {
        val applied = guarded(R.string.data_import_failed) { data.importBackup(json) }
        if (applied != null) _state.update { it.copy(message = R.string.data_import_done) }
    }

    suspend fun sessionsCsv(): String = CsvExport.sessions(sessions.observeAll().first())

    suspend fun injuriesCsv(): String = CsvExport.injuries(health.observeInjuries().first())

    suspend fun weightsCsv(): String = CsvExport.weights(health.observeWeights().first())

    suspend fun sessionsPdf(title: String): ByteArray {
        val rows = sessions.observeAll().first().filter { it.deletedAt == null }.sortedBy { it.date }.map { s ->
            listOf(s.date.toString(), s.type.name, s.durationMinutes.toString(), "${s.rankOnDay.belt.label} ${s.rankOnDay.degree}")
        }
        return PdfTableWriter.build(title, listOf("Data", "Tipo", "Min", "Faixa"), rows)
    }

    fun deleteAll() = viewModelScope.launch {
        val done = guarded(R.string.data_delete_failed) { data.deleteAllData() }
        if (done != null) _state.update { it.copy(deleted = true) }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private suspend fun <T> guarded(@StringRes failure: Int, block: suspend () -> T): T? {
        _state.update { it.copy(busy = true) }
        return try {
            block().also { _state.update { s -> s.copy(busy = false) } }
        } catch (error: DomainException) {
            _state.update { it.copy(busy = false, message = failure) }
            null
        } catch (error: IllegalArgumentException) {
            _state.update { it.copy(busy = false, message = failure) }
            null
        } catch (error: java.io.IOException) {
            _state.update { it.copy(busy = false, message = failure) }
            null
        }
    }
}
