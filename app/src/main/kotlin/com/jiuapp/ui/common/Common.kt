package com.jiuapp.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import java.util.Locale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jiuapp.R
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank

/** Espaçamentos do sistema de design. Nunca usar números soltos nas telas. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val touchTarget = 48.dp
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            content()
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(@StringRes message: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(message), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.fillMaxWidth().padding(Spacing.md),
    )
}

@Composable
fun MedicalDisclaimer(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.medical_disclaimer),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun KeyValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Nome da faixa no idioma do aparelho. */
@Composable
fun beltLabel(belt: Belt): String = stringResource(
    when (belt) {
        Belt.WHITE -> R.string.belt_white
        Belt.BLUE -> R.string.belt_blue
        Belt.PURPLE -> R.string.belt_purple
        Belt.BROWN -> R.string.belt_brown
        Belt.BLACK -> R.string.belt_black
        Belt.CORAL -> R.string.belt_coral
        Belt.RED -> R.string.belt_red
    },
)

@Composable
fun rankLabel(rank: Rank): String =
    if (rank.degree == 0) beltLabel(rank.belt) else "${beltLabel(rank.belt)} · ${rank.degree}º grau"

/** Locale atual lido pela configuração observada pelo Compose (recomposição ao trocar o idioma). */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
