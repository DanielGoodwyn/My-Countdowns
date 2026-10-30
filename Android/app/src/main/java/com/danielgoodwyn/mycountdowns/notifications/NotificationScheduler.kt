package com.danielgoodwyn.mycountdowns.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import androidx.core.app.NotificationCompat
import com.danielgoodwyn.mycountdowns.MainActivity
import com.danielgoodwyn.mycountdowns.R
import com.danielgoodwyn.mycountdowns.model.ResetItem

object NotificationScheduler {
    const val CHANNEL_ID = "countdown_channel"
    const val EXTRA_ID = "extra_countdown_id"
    const val EXTRA_NAME = "extra_countdown_name"
    const val EXTRA_IMAGE = "extra_countdown_image"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Countdown Notifications"
            val descriptionText = "Notifications when countdown times arrive"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun schedule(context: Context, item: ResetItem) {
        val now = System.currentTimeMillis()
        if (item.resetTime <= now) {
            cancel(context, item.id)
            return
        }

        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, CountdownAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ID, item.id)
            putExtra(EXTRA_NAME, item.name)
            putExtra(EXTRA_IMAGE, item.imageDataBase64)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val requestCode = item.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.resetTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.resetTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.resetTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, item.resetTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            alarmManager.set(AlarmManager.RTC_WAKEUP, item.resetTime, pendingIntent)
        }
    }

    fun cancel(context: Context, itemId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CountdownAlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, itemId.hashCode(), intent, flags)
        alarmManager.cancel(pendingIntent)
    }

    fun showNotification(context: Context, id: String, name: String, imageDataBase64: String?) {
        ensureChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var largeIconBitmap: Bitmap? = null
        if (!imageDataBase64.isNullOrEmpty()) {
            try {
                val decoded = Base64.decode(imageDataBase64, Base64.DEFAULT)
                largeIconBitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Countdown Complete")
            .setContentText("Your $name countdown is complete!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id.hashCode(), builder.build())
    }
}
