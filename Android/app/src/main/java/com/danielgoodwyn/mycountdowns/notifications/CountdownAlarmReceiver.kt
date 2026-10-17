package com.danielgoodwyn.mycountdowns.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CountdownAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(NotificationScheduler.EXTRA_ID) ?: return
        val name = intent.getStringExtra(NotificationScheduler.EXTRA_NAME) ?: "Countdown"
        val image = intent.getStringExtra(NotificationScheduler.EXTRA_IMAGE)
        NotificationScheduler.showNotification(context, id, name, image)
    }
}
