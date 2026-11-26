package com.riselock.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isTest = intent.getBooleanExtra("is_test", false)
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("is_test", isTest)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val cfg = AlarmPrefs.load(context)
            if (cfg.enabled) {
                AlarmPrefs.scheduleDailyAlarm(context, cfg)
            }
        }
    }
}
