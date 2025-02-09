package com.example.kotlinstart.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        when (intent?.action) {

            ProgressActionType.Pause.actionName -> {
                Log.d("test","receive ${ProgressActionType.Pause.actionName}")
                ProgressBinderService.progressBinder.pauseProgress()
            }

            ProgressActionType.Resume.actionName -> {
                Log.d("test","receive ${ProgressActionType.Resume.actionName}")
                ProgressBinderService.progressBinder.resumeProgress()
            }

            ProgressActionType.Hide.actionName -> {
                Log.d("test","receive ${ProgressActionType.Hide.actionName}")
                ProgressBinderService.progressBinder.stopForeground()
            }

        }
    }
}
