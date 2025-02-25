package com.example.kotlinstart.internal.android

import android.app.Notification
import android.app.NotificationManager
import android.app.Service

import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

import androidx.core.content.ContextCompat
import com.example.kotlinstart.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class ProgressActionType(val actionName: String){
    object Pause : ProgressActionType("ACTION_PAUSE")
    object Resume : ProgressActionType("ACTION_RESUME")
    object Hide : ProgressActionType("ACTION_HIDE")
}

class CountBinder : Binder() {
    private var count = 0
    fun getCount(): Int = count
    fun increaseCount() = count++
}

class ProgressBinder(private val service: ProgressBinderService = ProgressBinderService()) : Binder() {

    private var isForeground = false
    private var isPaused = true // 状态标志
    private var currentProgress: Float = 0f // 当前进度

    fun getPausedStatus(): Boolean = isPaused
    fun getForegroundStatus(): Boolean = isForeground

    fun getProgress(): Float = currentProgress

    fun getService(): ProgressBinderService = service

    fun pauseProgress(){
        isPaused = true
        getService().updateNotification()

    }

    fun resumeProgress(){
        isPaused = false
        getService().updateNotification()
    }

    fun increaseProgress(addProgress: Float){
        currentProgress += addProgress
    }

    fun stopForeground(){
        if(isForeground){
            getService().stopProgressForeground()
            isForeground = false
        }

    }

    fun startForeground(){
        if(!isForeground){
            getService().startProgressForeground()
            isForeground = true
        }

    }


}



class CountBinderService: Service() {

    companion object{
        lateinit var countBinder: CountBinder
    }

    fun getService() = this

    private val countBinder:CountBinder = CountBinder()

    override fun onCreate() = super.onCreate()
    override fun onBind(intent: Intent?): IBinder? = countBinder


}


class ProgressBinderService: Service() {

    private val notificationID = 1

    companion object{
        lateinit var progressBinder: ProgressBinder
    }

    private lateinit var notificationManager: NotificationManager
    private var currentNotificationBuilder : NotificationCompat.Builder? = null


    override fun onCreate(){
        super.onCreate()

        progressBinder = ProgressBinder(this)

        notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager


//        startProgressForeground()
        startProgressUpdate()

    }

    private fun startProgressUpdate() {

        CoroutineScope(Dispatchers.IO).launch {

            while (progressBinder.getProgress() < 100) {
                delay(1000) // 轮询刷新方式

                if (!progressBinder.getPausedStatus()) {
                    progressBinder.increaseProgress(1f)

                    if(progressBinder.getForegroundStatus()){
                        updateNotification()
                    }

                }

                //这个 notification 每当值更新一次 就要重新 rebuild整个 notification 是否有点。。??

            }

            stopProgressForeground()
            stopSelf()
        }

    }

    fun buildNotification(): Notification{

        val jumpIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("test://native/1105")
        )

        //暂停按钮
        val pauseIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            setAction(ProgressActionType.Pause.actionName)
        }

        // 继续按钮
        val resumeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            setAction(ProgressActionType.Resume.actionName)
        }

        // 隐藏按钮
        val hideIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            setAction(ProgressActionType.Hide.actionName)
        }

        val pendingIntent = MyChannel.defaultActivityIntent(context = this, intent = jumpIntent)
        val pausePendingIntent = MyChannel.defaultBroadcastIntent(context = this, intent = pauseIntent)
        val resumePendingIntent = MyChannel.defaultBroadcastIntent(context = this, intent = resumeIntent)
        val hidePendingIntent = MyChannel.defaultBroadcastIntent(context = this, intent = hideIntent)


        //exists
        currentNotificationBuilder?.let{
            Log.d("test","update ${progressBinder.getProgress().toInt()} %")
            return currentNotificationBuilder!!.run {
                    setProgress(100,progressBinder.getProgress().toInt(),false)
                    setContentText("当前进度: ${progressBinder.getProgress()} %")
                    clearActions()
                    addAction(
                        if (progressBinder.getPausedStatus()) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                        if (progressBinder.getPausedStatus()) "继续" else "暂停",
                        if (progressBinder.getPausedStatus()) resumePendingIntent else pausePendingIntent
                    )
                    addAction(
                        android.R.drawable.ic_media_play,
                        "隐藏",
                        hidePendingIntent
                    )
                    build()
            }
        }

        //new create
        currentNotificationBuilder = NotificationCompat.Builder(
            this,
            ChannelType.Progress.channelName
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("任务进度")
            .setContentText("当前进度: ${progressBinder.getProgress()} %")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .setContentIntent(pendingIntent)
            .setProgress(100,0,false)
            .addAction(
                if (progressBinder.getPausedStatus()) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (progressBinder.getPausedStatus()) "继续" else "暂停",
                if (progressBinder.getPausedStatus()) resumePendingIntent else pausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_media_play,
                "隐藏",
                hidePendingIntent
            )

        return currentNotificationBuilder!!.build()


    }


    fun startProgressForeground(){
        startForeground(notificationID, buildNotification())
    }

    fun stopProgressForeground(){
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun updateNotification() = notificationManager.notify(notificationID, buildNotification())

    override fun onBind(intent: Intent?): IBinder? = progressBinder

    override fun onDestroy() {
        super.onDestroy()
        stopProgressForeground()
        Log.d("test","progress service close")
    }


}

