package com.jiuapp.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jiuapp.MainActivity
import com.jiuapp.R
import com.jiuapp.domain.repository.SessionRepository
import com.jiuapp.domain.repository.SettingsRepository
import com.jiuapp.domain.stats.WeeklyGoal
import com.jiuapp.domain.stats.GoalStatus
import com.jiuapp.domain.stats.weekStart
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.first

@HiltWorker
class TrainingReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.observeSettings().first()
        val today = LocalDate.now(clock)
        if (!ReminderScheduler.shouldRemindOn(settings, today)) return Result.success()

        val weekSessions = sessionRepository.observeAll().first()
            .count { it.deletedAt == null && it.date.weekStart() == today.weekStart() }
        when (WeeklyGoal.status(weekSessions, settings.weeklyGoal, today)) {
            GoalStatus.BEHIND -> notify(
                channel = NotificationChannels.GOALS,
                id = GOAL_NOTIFICATION_ID,
                text = applicationContext.getString(R.string.notify_goal_behind, weekSessions, settings.weeklyGoal),
            )
            GoalStatus.ACHIEVED, GoalStatus.ON_TRACK -> notify(
                channel = NotificationChannels.REMINDERS,
                id = REMINDER_NOTIFICATION_ID,
                text = applicationContext.getString(R.string.notify_training_reminder),
            )
        }
        return Result.success()
    }

    private fun notify(channel: String, id: Int, text: String) {
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(
            applicationContext,
            id,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(applicationContext, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id, notification)
    }

    private companion object {
        const val GOAL_NOTIFICATION_ID = 1001
        const val REMINDER_NOTIFICATION_ID = 1002
    }
}
