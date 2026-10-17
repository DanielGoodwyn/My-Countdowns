package com.danielgoodwyn.mycountdowns.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.danielgoodwyn.mycountdowns.data.FirestoreManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-initialize FirestoreManager listeners which automatically reschedules pending alarms
            FirestoreManager.getInstance(context).startListening()
        }
    }
}
