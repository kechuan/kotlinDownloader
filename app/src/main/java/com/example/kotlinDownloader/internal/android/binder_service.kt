package com.example.kotlinDownloader.internal.android

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.kotlinDownloader.R
import com.example.kotlinDownloader.internal.DownloadTask
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager
import com.example.kotlinDownloader.internal.TaskStatus
import com.example.kotlinDownloader.internal.android.ProgressBinderService.Companion.progressBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlin.collections.firstOrNull


sealed class ProgressActionType(val actionName: String){
    object Pause : ProgressActionType("ACTION_PAUSE")
    object Resume : ProgressActionType("ACTION_RESUME")
    object Hide : ProgressActionType("ACTION_HIDE")
}

fun <T> defaultBindService(context: Context, createClass :Class<T>){
    context.bindService(
        Intent(context, createClass),
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
            override fun onServiceDisconnected(name: ComponentName?) {}
        },
        BIND_AUTO_CREATE
    )
}

class ProgressBinder(private val service: ProgressBinderService = ProgressBinderService()) : Binder() {

    private var isForeground = false
    private var isPaused = true // 状态标志
    private var currentProgress: Float = 0f // 当前进度

    fun isPausedStatus(): Boolean = isPaused
    fun getForegroundStatus(): Boolean = isForeground
    fun getProgress(): Float = currentProgress
    fun getService(): ProgressBinderService = service

    fun startProgressUpdate(){
        service.startProgressUpdate()
    }

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

class ProgressBinderService: Service() {

    private val notificationID = ChannelType.Progress.channelIndex

    companion object {
        lateinit var progressBinder: ProgressBinder
    }

    private lateinit var notificationManager: NotificationManager


    private fun buildNotification(): Notification{

        //自收自发
        val pauseIntent = Intent(this, ProgressBinderService::class.java).apply {
            setAction(ProgressActionType.Pause.actionName)
        }

        // 继续按钮
        val resumeIntent = Intent(this, ProgressBinderService::class.java).apply {
            setAction(ProgressActionType.Resume.actionName)
        }

        // 隐藏按钮
        val hideIntent = Intent(this, ProgressBinderService::class.java).apply {
            setAction(ProgressActionType.Hide.actionName)
        }

        val pausePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = pauseIntent)
        val resumePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = resumeIntent)
        val hidePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = hideIntent)


        //new create
        return progressNotification(
            context = this,
            channelName = ChannelType.Progress.channelName,
            progress = progressBinder.getProgress().toInt(),
            pausePendingIntent = pausePendingIntent,
            resumePendingIntent = resumePendingIntent,
            hidePendingIntent = hidePendingIntent,
            pausedStatus = progressBinder.isPausedStatus(),

        ).build()


    }


    fun startProgressUpdate() {

        CoroutineScope(Dispatchers.Default).launch {

            while (progressBinder.getProgress() < 100) {
                delay(1000) // 轮询刷新方式

                if (!progressBinder.isPausedStatus()) {
                    progressBinder.increaseProgress(1f)

                    if(progressBinder.getForegroundStatus()){
                        updateNotification()
                    }

                }

                //这个 notification 每当值更新一次 就要重新 rebuild整个 notification 是否有点。。??
                //原来在 kotlin 之前 就有 更新整体不可变的 思维了啊(

            }

            stopProgressForeground()
            stopSelf()
        }

    }

    fun startProgressForeground(){
        startForeground(notificationID, buildNotification())
    }

    fun stopProgressForeground(){
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun updateNotification() = notificationManager.notify(notificationID, buildNotification())


    override fun onCreate(){
        super.onCreate()

        progressBinder = ProgressBinder(this)

        notificationManager =
            ContextCompat.getSystemService(this, NotificationManager::class.java) as NotificationManager

    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        //而这个中转站是负责和 bind service 的方法 沟通的
        Log.d("test","receive intent:$intent")

        when (intent?.action) {

            ProgressActionType.Pause.actionName -> {
                Log.d("test","receive ${ProgressActionType.Pause.actionName}")
                progressBinder.pauseProgress()
            }

            ProgressActionType.Resume.actionName -> {
                Log.d("test","receive ${ProgressActionType.Resume.actionName}")
                progressBinder.resumeProgress()
            }

            ProgressActionType.Hide.actionName -> {
                Log.d("test","receive ${ProgressActionType.Hide.actionName}")
                progressBinder.stopForeground()
            }

        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = progressBinder

    override fun onDestroy() {
        super.onDestroy()
        stopProgressForeground()
        Log.d("test","progress service close")
    }


}

class TaskProgressBinder(private val service: TaskProgressBinderService = TaskProgressBinderService()) : Binder() {

    fun getService(): TaskProgressBinderService = service

    suspend fun pauseAllTask(context: Context) = MultiThreadDownloadManager.pauseAllTask(context)
    suspend fun resumeAllTask(context: Context) = MultiThreadDownloadManager.resumeAllTask(context)

    suspend fun pauseTask(
        context: Context,
        taskID: String
    ){
        MultiThreadDownloadManager.updateTaskStatus(
            context,
            taskID,
            TaskStatus.Paused
        )

    }

    suspend fun resumeTask(
        context: Context,
        taskID: String
    ){

        MultiThreadDownloadManager.updateTaskStatus(
            context,
            taskID,
            TaskStatus.Activating
        )

    }

    fun updateSummaryNotification() = getService().updateSummaryNotification()

    fun updateTaskNotification(
        taskID: String
    ) = getService().updateTaskNotification(taskID)

    //这个是 添加任意下载任务时 驱动
    fun startForeground(){
        Log.d("downloadTaskService","triggering service")

        //推送下载队列 还是 下载中? 还是队列罢.. 如果只是下载中 就不会出现 下载中 0 但是可以 resume 的诡异现象了
        getService().coroutineScope.launch {
            getService().startDownloadTaskForeground(
                getService().downloadingTaskFlow.value.map {
                    it.taskInformation.taskID
                }.toList()
            )
        }


    }


    //用户手动关闭时(Hide)驱动
    fun stopForeground() = getService().stopDownloadTaskForeground()


}

class TaskProgressBinderService: Service() {

    private var isForeground = false
    private val notificationID = ChannelType.TaskProgress.channelIndex

    private lateinit var notificationManager: NotificationManager

    val coroutineScope = CoroutineScope(Dispatchers.Default)

    val downloadingTaskFlow = MultiThreadDownloadManager.downloadingTaskFlow
//    val downloadingTaskCount = MultiThreadDownloadManager.downloadingTaskFlow.value.count()
    val finishedTaskCount = MultiThreadDownloadManager.finishedTaskFlow.value.count()

    companion object{
        lateinit var taskProgressBinder: TaskProgressBinder
    }

    fun getForegroundStatus() = isForeground

    private fun buildSummaryNotification(): Notification{

        val pauseAllIntent = Intent(this, TaskProgressBinderService::class.java).apply {
            setAction(ProgressActionType.Pause.actionName)
        }

        val resumeAllIntent = Intent(this, TaskProgressBinderService::class.java).apply {
            setAction(ProgressActionType.Resume.actionName)
        }

        val hideIntent = Intent(this, TaskProgressBinderService::class.java).apply {
            setAction(ProgressActionType.Hide.actionName)
        }

        val pausePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = pauseAllIntent)
        val resumePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = resumeAllIntent)
        val hidePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = hideIntent)

        return taskSummaryNotification(
            context = this,
            channelName = ChannelType.TaskProgress.channelName,
            downloadingQueueCount = downloadingTaskFlow.value.count(),
            finishedTaskCount = finishedTaskCount,
            pausePendingIntent = pausePendingIntent,
            resumePendingIntent = resumePendingIntent,
            hidePendingIntent = hidePendingIntent,

        ).build()
    }

    fun updateSummaryNotification(){
        notificationManager.notify(notificationID, buildSummaryNotification())
    }

    private fun buildTaskNotification(taskID: String): Notification{

        val jumpIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("downloader://$taskID")
        )

        //暂停按钮 Action区域
        val pauseIntent = Intent(this, TaskProgressBinderService::class.java).apply {
            setAction(ProgressActionType.Pause.actionName)
            setData(Uri.parse("downloader://$taskID"))
        }
        val resumeIntent = Intent(this, TaskProgressBinderService::class.java).apply {
            setAction(ProgressActionType.Resume.actionName)
            setData(Uri.parse("downloader://$taskID"))
        }


        val pendingIntent = MyChannel.defaultActivityIntent(context = this, intent = jumpIntent)
        val pausePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = pauseIntent)
        val resumePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = resumeIntent)

        return taskProgressNotification(
            context = this,
            channelName = ChannelType.Progress.channelName,
            downloadTask = MultiThreadDownloadManager.findTask(taskID),
            pausePendingIntent = pausePendingIntent,
            resumePendingIntent = resumePendingIntent,
            pendingIntent = pendingIntent,
        ).build()


    }


    fun updateTaskNotification(
        taskID: String
    ) = notificationManager.notify(taskID.toInt(), buildTaskNotification(taskID))

    fun startDownloadTaskForeground(
        downloadingTaskIDList: List<String?>
    ){

        if(!isForeground){

            startForeground(
                notificationID,
                buildSummaryNotification()
            )

            Log.d("downloadTaskService","service online")

            downloadingTaskIDList.mapIndexed { index,taskID ->

                MultiThreadDownloadManager.findTask(taskID)?.let{
                    notificationManager.notify(
                        taskID!!.toInt(),
                        buildTaskNotification(taskID)
                    )
                }

            }



            isForeground = true
        }

    }

    fun stopDownloadTaskForeground(){
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancelAll()
        isForeground = false
    }

    override fun onCreate(){
        super.onCreate()
        taskProgressBinder = TaskProgressBinder(this)

        notificationManager =
            ContextCompat.getSystemService(this, NotificationManager::class.java) as NotificationManager

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        //而这个中转站是负责和 bind service 的方法 沟通的

        coroutineScope.launch {
            //  segments://Host/Path
            //downloader://123456
            when (intent?.action) {

                ProgressActionType.Pause.actionName -> {
                    if(intent.data == null) taskProgressBinder.pauseAllTask(taskProgressBinder.getService())
                    else{
                        intent.data?.host?.let {
                            taskProgressBinder.pauseTask(context = taskProgressBinder.getService(),it)
                        }
                    }

                }

                ProgressActionType.Resume.actionName -> {
                    if(intent.data == null) taskProgressBinder.resumeAllTask(taskProgressBinder.getService())
                    else{
                        intent.data?.host?.let {
                            taskProgressBinder.resumeTask(context = taskProgressBinder.getService(),it)
                        }
                    }
                }

                ProgressActionType.Hide.actionName -> {
                    taskProgressBinder.stopForeground()
                }

            }
        }



        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = taskProgressBinder

    override fun onDestroy() {
        super.onDestroy()
        stopDownloadTaskForeground()
        Log.d("DownloadTaskService","progress service close")
    }


}
