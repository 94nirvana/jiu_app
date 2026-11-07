package com.jiuapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import android.content.Intent
import com.jiuapp.MainActivity
import com.jiuapp.R
import com.jiuapp.domain.repository.SessionRepository
import com.jiuapp.domain.repository.SettingsRepository
import com.jiuapp.domain.stats.GoalStatus
import com.jiuapp.domain.stats.WeeklyGoal
import com.jiuapp.domain.stats.weekStart
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/** Resumo da semana: treinos e meta. Somente leitura, sem ações de registro. */
class WeekSummaryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val today = LocalDate.now(entry.clock())
        val sessions = entry.sessions().observeAll().first()
        val goal = entry.settings().observeSettings().first().weeklyGoal
        val week = sessions.count { it.deletedAt == null && it.date.weekStart() == today.weekStart() }
        val statusText = when (WeeklyGoal.status(week, goal, today)) {
            GoalStatus.ACHIEVED -> context.getString(R.string.goal_achieved)
            GoalStatus.ON_TRACK -> context.getString(R.string.goal_on_track)
            GoalStatus.BEHIND -> context.getString(R.string.goal_behind)
        }
        val title = context.getString(R.string.widget_title)
        val progress = context.getString(R.string.widget_progress, week, goal)
        val openApp = Intent(context, MainActivity::class.java)
        provideContent {
            GlanceTheme {
                Summary(title = title, progress = progress, status = statusText, openApp = openApp)
            }
        }
    }
}

@Composable
private fun Summary(title: String, progress: String, status: String, openApp: Intent) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp).clickable(actionStartActivity(openApp)),
    ) {
        Text(text = title, style = TextStyle(fontWeight = FontWeight.Bold))
        Text(text = progress)
        Text(text = status)
    }
}

class WeekSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekSummaryWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun sessions(): SessionRepository
    fun settings(): SettingsRepository
    fun clock(): Clock
}
