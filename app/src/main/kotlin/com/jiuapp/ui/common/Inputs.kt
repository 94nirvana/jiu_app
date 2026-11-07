package com.jiuapp.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import com.jiuapp.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Campo de data que abre o seletor do Material 3. Não aceita digitação livre, evitando datas inválidas. */
@Composable
fun DateField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth().clickable(role = Role.Button) { open = true }) {
        // Desabilitado para que o toque chegue ao Box e abra o seletor; o texto continua legível.
        OutlinedTextField(
            value = value?.toString().orEmpty(),
            onValueChange = {},
            enabled = false,
            label = { Text(label) },
            placeholder = { Text(stringResource(R.string.date_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (value ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onValueChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                Row {
                    if (allowClear) {
                        TextButton(onClick = { onValueChange(null); open = false }) { Text(stringResource(R.string.action_clear)) }
                    }
                    TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel)) }
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Seleção de uma opção entre várias, em linha com quebra. Cada opção é um alvo de toque de 48dp. */
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        options.chunked(4).forEach { row ->
            Row {
                row.forEach { option ->
                    FilterChip(
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        label = { Text(label(option)) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp).height(48.dp),
                    )
                }
            }
        }
    }
}

/** Escala 1 a 5 com rótulos acessíveis. `null` representa "não informado". */
@Composable
fun ScaleChoice(
    title: String,
    value: Int?,
    modifier: Modifier = Modifier,
    onValueChange: (Int?) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        ChoiceChips(
            options = listOf(1, 2, 3, 4, 5),
            selected = value,
            label = { it.toString() },
            onSelect = { onValueChange(if (it == value) null else it) },
        )
    }
}

/** Seleção múltipla em chips. Cada opção é um alvo de toque de 48dp. */
@Composable
fun <T> MultiChoiceChips(
    options: List<T>,
    selected: Set<T>,
    label: @Composable (T) -> String,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onToggle(option) },
                label = { Text(label(option)) },
                modifier = Modifier.height(48.dp),
            )
        }
    }
}
