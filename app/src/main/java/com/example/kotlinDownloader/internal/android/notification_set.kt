package com.example.kotlinDownloader.internal.android

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

import androidx.core.content.ContextCompat.getSystemService
import com.example.kotlinDownloader.R
import com.example.kotlinDownloader.internal.DownloadTask
import com.example.kotlinDownloader.internal.TaskStatus
import com.example.kotlinDownloader.internal.android.ProgressBinderService.Companion.progressBinder
import com.example.kotlinDownloader.internal.android.TaskProgressBinderService.Companion.taskProgressBinder
import com.example.kotlinDownloader.internal.convertBinaryType
import com.example.kotlinDownloader.internal.convertDownloadedSize

sealed class ChannelType(val channelName:String,val channelIndex:Int){
    object Example: ChannelType("channelExample",1)
    object Progress: ChannelType("channelShowProgress",2)
    object TaskProgress: ChannelType("channelTaskProgress",3)
}

const val downloadTaskGroupKey = "kotlinDownloader.download_task_group"

object MyChannel{

    fun defaultActivityIntent (
        context: Context,
        intent: Intent
    ): PendingIntent? = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    fun defaultServiceIntent (
        context: Context,
        intent: Intent
    ): PendingIntent? = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    fun defaultBroadcastIntent (
        context: Context,
        intent: Intent
    ): PendingIntent? = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)


    fun init(context:Context){

//        registerChannel(
//            context = context,
//            channelID = ChannelType.Example.channelName,
//            channelName = "简单通知通道",
//            descriptionName = "这是一个示例通知通道",
//            importance = NotificationManager.IMPORTANCE_HIGH
//        )

        registerChannel(
            context = context,
            channelID = ChannelType.Progress.channelName,
            channelName = "进度条通知",
            descriptionName = "这是一个示例进度条通道",
            importance = NotificationManager.IMPORTANCE_HIGH
        )

        registerChannel(
            context = context,
            channelID = ChannelType.TaskProgress.channelName,
            channelName = "任务进度通知",
            descriptionName = "这是一个下载时的前台服务通知",
            importance = NotificationManager.IMPORTANCE_HIGH
        )

//        registerChannel(
//            context = context,
//            channelID = ChannelType.Alarm.channelName,
//            channelName = "提醒通知",
//            importance = NotificationManager.IMPORTANCE_HIGH
//        )

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
        notificationManager!!.createNotificationChannel(channel)
    }

}

fun verifyNoticePermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.areNotificationsEnabled()
    }

fun requestNoticePermission(context: Context): Boolean{
    if ( verifyNoticePermission(context) ) return true

    else{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                context as Activity,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            ).run{
                return@requestNoticePermission verifyNoticePermission(context)
            }
        }

    }

    return false
}

fun progressNotification(
    context: Context,
    channelName: String,
    progress: Int,
    pausePendingIntent: PendingIntent? = null,
    resumePendingIntent: PendingIntent? = null,
    hidePendingIntent: PendingIntent? = null,
    pausedStatus: Boolean
): NotificationCompat.Builder {

    val notificationBuilder =  NotificationCompat.Builder(
        context,
        channelName
    )

    notificationBuilder.run{
        setSmallIcon(R.drawable.ic_launcher_foreground)
        setContentTitle("任务进度")
        setContentText("当前进度: $progress %")
        setProgress(100,progress,false)
        setOngoing(true)
        pausePendingIntent?.let {
            addAction(
                if (pausedStatus) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (pausedStatus) "继续" else "暂停",
                if (pausedStatus) resumePendingIntent else pausePendingIntent
            )
        }

        hidePendingIntent?.let{
            addAction(
                android.R.drawable.ic_media_play,
                "隐藏",
                hidePendingIntent
            )
        }

    }

    return notificationBuilder
}

fun taskSummaryNotification(
    context: Context,
    channelName: String,
    downloadingQueueCount: Int = 0,
    finishedTaskCount: Int = 0,
    hidePendingIntent: PendingIntent? = null,
    pausePendingIntent: PendingIntent? = null,
    resumePendingIntent: PendingIntent? = null,

): NotificationCompat.Builder {
    val notificationBuilder = NotificationCompat.Builder(
        context,
        channelName
    )

    notificationBuilder.apply {
        setContentTitle("在下载队列中: $downloadingQueueCount | 已完成 $finishedTaskCount")
        setSmallIcon(R.drawable.ic_launcher_foreground)
        setOngoing(true)
        setGroup(downloadTaskGroupKey)

        hidePendingIntent?.let {
            addAction(
                android.R.drawable.ic_media_play,
                "隐藏",
                hidePendingIntent
            )
        }

        pausePendingIntent?.let {

            addAction(
                android.R.drawable.ic_media_pause,
                "全部暂停",
                pausePendingIntent
            )

        }

        resumePendingIntent?.let{
            addAction(
                android.R.drawable.ic_media_play,
                "全部恢复",
                resumePendingIntent
            )
        }
    }

    return notificationBuilder
}


fun taskProgressNotification(
    context: Context,
    channelName: String,
    downloadTask: DownloadTask? = null,
    pendingIntent: PendingIntent? = null,
    pausePendingIntent: PendingIntent? = null,
    resumePendingIntent: PendingIntent? = null,
): NotificationCompat.Builder {

    val notificationBuilder =  NotificationCompat.Builder(
        context,
        channelName
    )

    notificationBuilder.run{
        //通用
        setSmallIcon(R.drawable.ic_launcher_foreground)
        setGroup(downloadTaskGroupKey)

        // progress目前暂且设置: 当无任务/多个任务时 currentProgress 则为null
        // 即只显示单个任务的 progress/speed
        downloadTask?.let{
            val progress = convertDownloadedSize(
                chunksRangeList = it.taskInformation.chunksRangeList,
                chunkProgress = it.chunkProgress
            ).toFloat() / it.taskInformation.fileSize

            setContentTitle(downloadTask.taskInformation.fileName)

            if(downloadTask.taskStatus == TaskStatus.Finished){
                setContentText("已完成")
                setOngoing(false)
                setAutoCancel(true)
            }

            else{
                setProgress(100,(progress*100).toInt(),false)
                setContentText("${(progress*100).toInt()} % ${convertBinaryType(it.currentSpeed)}/s")
                setOngoing(true)
            }


            pausePendingIntent?.let {
                if(downloadTask.taskStatus != TaskStatus.Finished){
                    addAction(
                        if (downloadTask.taskStatus != TaskStatus.Activating) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                        if (downloadTask.taskStatus != TaskStatus.Activating) "恢复" else "暂停",
                        if (downloadTask.taskStatus != TaskStatus.Activating) resumePendingIntent else pausePendingIntent
                    )
                }
            }

        }

        pendingIntent?.let {
            setContentIntent(it)
        }



    }

    return notificationBuilder
}

