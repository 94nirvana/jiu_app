package com.jiuapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.jiuapp.R

/** Canais separados para que o usuário controle cada tipo de aviso nas configurações do sistema. */
object NotificationChannels {

    const val REMINDERS = "reminders"
    const val GOALS = "goals"
    const val COMPETITIONS = "competitions"
    const val HEALTH = "health"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(GOALS, context.getString(R.string.channel_goals), NotificationManager.IMPORTANCE_LOW),
            NotificationChannel(COMPETITIONS, context.getString(R.string.channel_competitions), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(HEALTH, context.getString(R.string.channel_health), NotificationManager.IMPORTANCE_DEFAULT),
        )
        manager.createNotificationChannels(channels)
    }

    fun canNotify(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()
}
