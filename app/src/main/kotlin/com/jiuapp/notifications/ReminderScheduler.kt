package com.jiuapp.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jiuapp.domain.settings.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agenda uma verificação diária no horário escolhido. O worker decide se há aviso para o dia
 * (dias configurados e meta semanal). Trabalho local: não exige rede.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    fun reschedule(settings: AppSettings) {
        val workManager = WorkManager.getInstance(context)
        if (!settings.reminderEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.ofInstant(clock.instant(), zone)
        var next = now.toLocalDate().atTime(settings.reminderTime)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next)

        val request = PeriodicWorkRequestBuilder<TrainingReminderWorker>(REPEAT_HOURS, TimeUnit.HOURS)
            .setInitialDelay(delay)
            .setConstraints(Constraints.NONE)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    companion object {
        const val WORK_NAME = "training-reminder"
        const val REPEAT_HOURS = 24L

        /** O worker só avisa nos dias configurados. Função pura para facilitar testes. */
        fun shouldRemindOn(settings: AppSettings, date: LocalDate): Boolean =
            settings.reminderEnabled && date.dayOfWeek in settings.reminderDays
    }
}
