package com.example.kotlinDownloader.internal.android

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
    private var currentNotificationBuilder : NotificationCompat.Builder? = null


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

    private fun buildNotification(): Notification{

        val jumpIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("test://native/1105")
        )

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

        val pendingJumpIntent = MyChannel.defaultActivityIntent(context = this, intent = jumpIntent)
        val pausePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = pauseIntent)
        val resumePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = resumeIntent)
        val hidePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = hideIntent)

        //exists
        currentNotificationBuilder?.let{
            Log.d("test","update ${progressBinder.getProgress().toInt()} %")
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

        //new create
        currentNotificationBuilder = progressNotification(
            context = this,
            channelName = ChannelType.Progress.channelName,
            progress = 0,
            pausePendingIntent = pausePendingIntent,
            resumePendingIntent = resumePendingIntent,
            hidePendingIntent = hidePendingIntent,
            pausedStatus = progressBinder.isPausedStatus(),

        )

        return currentNotificationBuilder!!.build()

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

    override fun onBind(intent: Intent?): IBinder? = progressBinder

    override fun onDestroy() {
        super.onDestroy()
        stopProgressForeground()
        Log.d("test","progress service close")
    }


}

class TaskProgressBinder(private val service: TaskProgressBinderService = TaskProgressBinderService()) : Binder() {

    private var isForeground = false

    //计划:仅当任务只有1个时 可以使用progress数据
    // 若任务不为1时 则变成 暂停/恢复 所有任务
    // 同理 已经暂停时 此时恢复任意1个 都会在通知里变成 单任务活跃状态

    //状态显示是单向的 故 此处不再设置 private

    val coroutineScope = CoroutineScope(Dispatchers.Default)

    var targetDownloadTask: DownloadTask? = null

    var currentProgress: Int? = null // 当前进度(仅限单个任务可查看)

    val downloadingTaskFlow = MultiThreadDownloadManager.downloadingTaskFlow

    val activeTaskCount = downloadingTaskFlow.value.count {
        it.taskStatus == TaskStatus.Activating
    }

    val finishedTaskCount = MultiThreadDownloadManager.finishedTaskFlow.value.count {
        it.taskStatus == TaskStatus.Activating
    }


    fun getService(): TaskProgressBinderService = service

    suspend fun pauseTask(taskID: String){
        Log.d("service","pause $taskID")

//        MultiThreadDownloadManager.pauseAllTask(service).run {
//            getService().updateNotification(taskID)
//        }
//
//
//        MultiThreadDownloadManager.pauseAllTask(service).run {
//            getService().updateNotification(taskID)
//        }
    }

    suspend fun resumeTask(taskID: String){
        Log.d("service","resume $taskID")
//        MultiThreadDownloadManager.resumeAllTask(service).run {
//            getService().updateNotification(taskID)
//        }

    }

    //这个是 添加任意下载任务时 驱动
    fun startForeground(){
        if(!isForeground){
            coroutineScope.launch{
                getService().startDownloadTaskForeground(
                    downloadingTaskFlow.map {
                        it.firstOrNull { it.taskStatus == TaskStatus.Activating }?.taskInformation?.taskID
                    }.toList()
                )
            }

            isForeground = true
        }
    }

    //用户手动关闭时(Hide)驱动
    fun stopForeground(){
        if(isForeground){
            getService().stopDownloadTaskForeground()
            isForeground = false
        }

    }




}

class TaskProgressBinderService: Service() {



    private val notificationID = ChannelType.TaskProgress.channelIndex

    companion object{
        lateinit var taskProgressBinder: TaskProgressBinder
    }

    private lateinit var notificationManager: NotificationManager
    private var currentNotificationBuilder : NotificationCompat.Builder? = null

    private fun buildSummaryNotification(): Notification{

        val hideIntent = Intent(this, TaskProgressBinderService::class.java).apply {
            setAction(ProgressActionType.Hide.actionName)
        }

        val hidePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = hideIntent)

        return taskSummaryNotification(
            context = this,
            channelName = ChannelType.Progress.channelName,
            hidePendingIntent = hidePendingIntent,
        ).build()
    }

    private fun buildTaskNotification(taskID: String): Notification{

        val jumpIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("downloader://$taskID")
        )

        //暂停按钮 Action区域
        val pauseIntent = Intent(this, TaskProgressBinderService::class.java).apply {
            setAction(ProgressActionType.Pause.actionName)
        }
        val resumeIntent = Intent(this, TaskProgressBinderService::class.java).apply {
            setAction(ProgressActionType.Resume.actionName)
        }


        val pendingIntent = MyChannel.defaultActivityIntent(context = this, intent = jumpIntent)
        val pausePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = pauseIntent)
        val resumePendingIntent = MyChannel.defaultServiceIntent(context = this, intent = resumeIntent)


        //exists
        currentNotificationBuilder?.let{
            Log.d("taskProgressService","${taskProgressBinder.activeTaskCount} update ${taskProgressBinder.currentProgress?.toInt()} %")

            return taskProgressNotification(
                context = this,
                channelName = ChannelType.Progress.channelName,
                downloadTask = null,
                pausePendingIntent = pausePendingIntent,
                resumePendingIntent = resumePendingIntent,
                pendingIntent = pendingIntent,
//                hidePendingIntent = hidePendingIntent,
                pausedStatus = false,
            ).build()

            //当无任务/多个任务时 currentProgress 则为null

        }

        //new create
        // create的时机一般应是。。刚触发下载时 直白点讲就是 activeTaskCount 0 => 1 时
        currentNotificationBuilder = taskProgressNotification(
            context = this,
            channelName = ChannelType.Progress.channelName,
            downloadTask = MultiThreadDownloadManager.findTask(taskID),
            pausePendingIntent = pausePendingIntent,
            resumePendingIntent = resumePendingIntent,
//            hidePendingIntent = hidePendingIntent,
            pausedStatus = false,
        )

        return currentNotificationBuilder!!.build()

    }

    fun updateNotification(
        taskID: String,
        taskOrder: Int,
    ) = notificationManager.notify(notificationID+taskOrder, buildTaskNotification(taskID))

    fun startDownloadTaskForeground(
        taskIDList: List<String?>
    ){

        startForeground(
            notificationID,
            buildSummaryNotification()
        )


        taskIDList.mapIndexed { index,taskID ->
            taskID?.let{
                notificationManager.notify(
                    notificationID+(index+1),
                    buildTaskNotification(taskID)
                )
            }

        }



    }

    fun stopDownloadTaskForeground(){
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onCreate(){
        super.onCreate()
        taskProgressBinder = TaskProgressBinder(this)

        notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        //而这个中转站是负责和 bind service 的方法 沟通的

        taskProgressBinder.coroutineScope.launch {
            when (intent?.action) {

                ProgressActionType.Pause.actionName -> {
                    intent.data?.path?.let {
                        taskProgressBinder.pauseTask(it)
                    }
                }

                ProgressActionType.Resume.actionName -> {
                    intent.data?.path?.let {
                        taskProgressBinder.resumeTask(it)
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
