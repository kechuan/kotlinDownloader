package com.example.kotlinstart.internal

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import com.example.kotlinstart.R

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService

import kotlin.random.Random


sealed class ChannelType(val channelName :String){
    object Example: ChannelType("channelExample")
    object Progress: ChannelType("channelShowProgress")
    object Alarm: ChannelType("channelAlarm")
}

object MyChannel{
    fun defaultActivityIntent (
        context: Context,
        intent: Intent
    ) = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    fun defaultBroadcastIntent (
        context: Context,
        intent: Intent
    ) = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)


    fun init(context:Context){

        registerChannel(
            context = context,
            channelID = ChannelType.Example.channelName,
            channelName = "简单通知通道",
            descriptionName = "这是一个示例通知通道",
            importance = NotificationManager.IMPORTANCE_HIGH
        )

        registerChannel(
            context = context,
            channelID = ChannelType.Progress.channelName,
            channelName = "进度条通知",
            importance = NotificationManager.IMPORTANCE_HIGH
        )

        registerChannel(
            context = context,
            channelID = ChannelType.Alarm.channelName,
            channelName = "提醒通知",
            importance = NotificationManager.IMPORTANCE_HIGH
        )

    }

    fun registerChannel(
        context:Context,
        channelID: String,
        channelName: String,
        descriptionName: String? = null,
        importance: Int,

    ){

        val notificationManager = getSystemService(
            context,
            NotificationManager::class.java
        )

        val channel = NotificationChannel(channelID, channelName, importance).apply {
            descriptionName?.let{
                description = descriptionName
            }

        }

        //创立channel
        notificationManager!!.createNotificationChannel(
            channel
        )
    }

}

// Notification & Receiver
class NotificationPublisher : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Reminder"
        val message = intent.getStringExtra("message") ?: "Time's up!"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, ChannelType.Alarm.channelName)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }

}

// 修改调度逻辑，使用 AlarmManager
fun receiverScheduleNotification(
    context: Context,
    triggerTime: Long,
    title: String,
    message: String
) {

    val intent = Intent(context, NotificationPublisher::class.java).apply {
        putExtra("title", title)
        putExtra("message", message)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        Random.nextInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 使用 set
    alarmManager.set(
        AlarmManager.RTC_WAKEUP,
        triggerTime,
        pendingIntent
    )
}





