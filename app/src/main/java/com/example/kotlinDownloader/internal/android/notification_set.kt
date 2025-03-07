package com.example.kotlinDownloader.internal.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat

import androidx.core.content.ContextCompat.getSystemService
import com.example.kotlinDownloader.R
import com.example.kotlinDownloader.internal.DownloadTask
import com.example.kotlinDownloader.internal.android.ProgressBinderService.Companion.progressBinder
import com.example.kotlinDownloader.internal.android.TaskProgressBinderService.Companion.taskProgressBinder
import com.example.kotlinDownloader.internal.convertBinaryType
import com.example.kotlinDownloader.internal.convertDownloadedSize



sealed class ChannelType(val channelName:String,val channelIndex:Int){
    object Example: ChannelType("channelExample",1)
    object Progress: ChannelType("channelShowProgress",2)
    object Alarm: ChannelType("channelAlarm",3)
    object TaskProgress: ChannelType("channelTaskProgress",1024)
}

const val downloadTaskGroupKey = "kotlinDownloader.TASK_GROUP"

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
            descriptionName = "这是一个示例进度条通道",
            importance = NotificationManager.IMPORTANCE_HIGH
        )

        registerChannel(
            context = context,
            channelID = ChannelType.TaskProgress.channelName,
            channelName = "任务进度通知",
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

fun progressNotification(
    context: Context,
    channelName: String,
    progress: Int,
    pausePendingIntent: PendingIntent?,
    resumePendingIntent: PendingIntent?,
    hidePendingIntent: PendingIntent?,
    pausedStatus: Boolean
): NotificationCompat.Builder {

    val notificationBuilder =  NotificationCompat.Builder(
        context,
        channelName
    )
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("任务进度")
        .setContentText("当前进度: $progress %")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setProgress(100,progress,false)
        .clearActions()
        .setOngoing(true)

    pausePendingIntent?.let {
        notificationBuilder.addAction(
            if (pausedStatus) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
            if (pausedStatus) "继续" else "暂停",
            if (pausedStatus) resumePendingIntent else pausePendingIntent
        )
    }

    hidePendingIntent?.let{
        notificationBuilder.addAction(
            android.R.drawable.ic_media_play,
            "隐藏",
            hidePendingIntent
        )
    }


    return notificationBuilder
}

fun taskSummaryNotification(
    context: Context,
    channelName: String,
    activeTaskCount: Int = 0,
    finishedTaskCount: Int = 0,
    hidePendingIntent: PendingIntent? = null

): NotificationCompat.Builder {
    val notificationBuilder = NotificationCompat.Builder(
        context,
        channelName
    )

    notificationBuilder.run {
        setContentTitle("在下载队列中: $activeTaskCount | 已完成 $finishedTaskCount")
        setSmallIcon(R.drawable.ic_launcher_foreground)

            //TODO 传入 Flow?
            .setStyle(
                NotificationCompat.InboxStyle()

                    .addLine("Task1")
                    .addLine("Task2")
            )

        setGroup(downloadTaskGroupKey)
        setGroupSummary(true)

        hidePendingIntent?.let {
            addAction(
                android.R.drawable.ic_media_play,
                "隐藏",
                hidePendingIntent
            )
        }
    }

    return notificationBuilder
}




fun taskProgressNotification(
    context: Context,
    channelName: String,
    downloadTask: DownloadTask? = null,
    pausedStatus: Boolean,
    pendingIntent: PendingIntent? = null,
    pausePendingIntent: PendingIntent? = null,
    resumePendingIntent: PendingIntent? = null,
//    hidePendingIntent: PendingIntent? = null,
): NotificationCompat.Builder {

    val notificationBuilder =  NotificationCompat.Builder(
        context,
        channelName
    )

    notificationBuilder.run{
        //通用
        setSmallIcon(R.drawable.ic_launcher_foreground)

        // [setContentTitle]: 通用 如果能分离。。
        // 我的意思说 如果能监听 全局 的 总体速度
        // 那么这里就可以改改 改成显示每个任务的 Progress / Speed
        // 不然整不出花活。否则就得每个任务分离显示一个通知 这不好。。


        clearActions()

        // progress目前暂且设置: 当无任务/多个任务时 currentProgress 则为null
        // 即只显示单个任务的 progress/speed
        downloadTask?.let{
            val progress = convertDownloadedSize(
                chunksRangeList = it.taskInformation.chunksRangeList,
                chunkProgress = it.chunkProgress
            ).toFloat() / it.taskInformation.fileSize

            setProgress(100,progress.toInt(),false)
            setContentText("${taskProgressBinder.currentProgress?.toInt()} % ${convertBinaryType(it.currentSpeed)}/s")
        }

        pendingIntent?.let {
            setContentIntent(it)
        }

        pausePendingIntent?.let {
            addAction(
                if (pausedStatus) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (pausedStatus) "${if(downloadTask==null) "全部" else ""}恢复"
                else "${if(downloadTask==null) "全部" else ""}暂停",
                if (pausedStatus) resumePendingIntent else pausePendingIntent
            )
        }



        setGroup(downloadTaskGroupKey)

    }

    return notificationBuilder
}


//// 修改调度逻辑，使用 AlarmManager
//fun receiverScheduleNotification(
//    context: Context,
//    triggerTime: Long,
//    title: String,
//    message: String
//) {
//
//    val intent = Intent(context, NotificationPublisher::class.java).apply {
//        putExtra("title", title)
//        putExtra("message", message)
//    }
//
//    val pendingIntent = PendingIntent.getBroadcast(
//        context,
//        Random.nextInt(),
//        intent,
//        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//    )
//
//    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//    // 使用 set
//    alarmManager.set(
//        AlarmManager.RTC_WAKEUP,
//        triggerTime,
//        pendingIntent
//    )
//}
//




