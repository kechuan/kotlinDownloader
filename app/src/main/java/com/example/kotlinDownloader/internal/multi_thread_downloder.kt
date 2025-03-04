package com.example.kotlinDownloader.internal

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import co.touchlab.stately.concurrency.value
import com.example.kotlinDownloader.models.DownloadViewModel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.Headers
import okhttp3.ResponseBody
import retrofit2.Response


import java.io.FileOutputStream
import java.io.IOException

import java.net.URL
import java.time.Instant
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicInteger

import kotlin.text.toLongOrNull

typealias ProgressCallback = (current: Long, total: Long) -> Unit

object MultiThreadDownloadManager: ViewModel() {

    val retrofitClient = HttpRequestClient.githubClient
    private val downloadRequest = retrofitClient.create(DownloadApi::class.java)

    private val scope = CoroutineScope(Dispatchers.IO)
    private val jobs = mutableListOf<Job>()

    private val downloadingTaskState = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadingTaskFlow: StateFlow<List<DownloadTask>> = downloadingTaskState.asStateFlow()

    private val finishedTaskState = MutableStateFlow<List<DownloadTask>>(emptyList())
    val finishedTaskFlow = finishedTaskState.asStateFlow()

    suspend fun init(context: Context){

        val storageTasksFlow = MyDataStore.getAllTasks(context)

        downloadingTaskState.value = storageTasksFlow.first().filter{
            it.taskStatus != TaskStatus.Finished
        }.map {

            if(it.taskStatus == TaskStatus.Activating){
                it.copy(taskStatus = TaskStatus.Paused)
            }

            else{
                it
            }
        }

        finishedTaskState.value = storageTasksFlow.first().filter {
            it.taskStatus == TaskStatus.Finished
        }

        storageTasksFlow.collect{
            Log.d("taskDataStore","updated at ${System.currentTimeMillis()}")
        }


    }


    // 添加下载任务
    @OptIn(DelicateCoroutinesApi::class)
    fun addTask(
        context: Context,
        downloadTask: DownloadTask?,
        threadCount: Int = 5,
        isResume: Boolean = false
    ) {

        downloadTask?.let{ downloadTask ->
            //不存在任务时添加
            if(!jobs.any{
                //outside name
                //@Deprecated Job.coroutineContext[CoroutineName]?.name
                val jobName = (it as CoroutineScope).coroutineContext[CoroutineName]?.name
                jobName == downloadTask.taskInformation.taskID
            }){

                val job = CoroutineScope(
                    Dispatchers.IO +
                    CoroutineName(downloadTask.taskInformation.taskID)
                ).launch {
                    //inside name
                    Log.d("taskInfo","job ScopeName name: ${coroutineContext[CoroutineName]?.name} / ${downloadTask.taskInformation.downloadUrl.hashCode()}")

                    try {

                        var fileSize = 0L
                        lateinit var chunksRangeList: List<Pair<Long, Long>>
                        lateinit var chunkDownloadedList: MutableList<Int>

                        if(!isResume){
                            //没有三目表达式的情况之下 真的很不喜欢把 if/else 写在一个变量定义里
                            //假如其已经在dialog里获取到信息就直接沿用它的信息
                            if( downloadTask.taskInformation.fileSize != 0L){
                                fileSize = downloadTask.taskInformation.fileSize
                            }

                            else{

                                //TODO 未知文件大小的处理
                                fileSize = async { getFileSize(
                                    downloadUrl = downloadTask.taskInformation.downloadUrl,
                                    chunkRequestFallback = {

                                        var contentLength = emptyLength

                                        launch {
                                            withContext(Dispatchers.IO){
                                                Log.d("taskInfo","trigger fallback contentGET")

                                                Log.d("taskInfo","backup Request")
                                                val response = downloadRequest.getCustomUrl(downloadTask.taskInformation.downloadUrl,"bytes=0-1")
                                                Log.d("taskInfo","backup Request done.")

                                                if(response.isSuccessful){
                                                    val headers = response.headers()
                                                    Log.d("taskInfo","GET Request Info:$headers")

                                                    // example: content-range: bytes 0-1/144534
                                                    contentLength = headers["content-range"]?.split("/")?.last()?.toLongOrNull() ?: emptyLength

                                                    Log.d("taskInfo","contentLength: $contentLength")

                                                    if(contentLength == null){
                                                        //TODO Toaster
                                                        Log.d("taskInfo","fail to get Size.Quit")
                                                    }
                                                }


                                            }
                                        }


                                        return@getFileSize contentLength

                                    }
                                ) }.await().also {

                                    Log.d("taskInfo","fileName : ${downloadTask.taskInformation.fileName} contentLength Length: $it.")

                                    if(it == null) {
                                        Log.d("taskInfo","contentLength get failed. cancel Task.")
                                        cancelJob(downloadTask.taskInformation.taskID)
                                    }
                                }  ?: return@launch


                            }

                            //UI任务显示: 初始化

                            Log.d("taskInfo","start allocChunks, UI Show Task")

                            downloadingTaskState.value = downloadingTaskFlow.value + downloadTask
                            chunksRangeList = calculateChunks(fileSize, threadCount)
                            chunkDownloadedList = MutableList<Int>(chunksRangeList.size){0}

                            Log.d("taskInfo","chunkList: $chunksRangeList")

                            val latestDownloadTask = downloadTask.copy(
                                taskInformation = downloadTask.taskInformation.copy(
                                    chunksRangeList = chunksRangeList,
                                    fileSize = fileSize,
                                ),
                                chunkProgress = chunkDownloadedList,

                                taskStatus = TaskStatus.Activating
                            )

                            updateTaskInformation(
                                taskID = latestDownloadTask.taskInformation.taskID,
                                propTransform = { latestDownloadTask }
                            )

                            MyDataStore.addTask(
                                context = context,
                                newTask = latestDownloadTask
                            )

                            preallocateSpace(
                                context = context,
                                uri = Uri.parse(downloadTask.taskInformation.storagePath),
                                targetSize = fileSize
                            ).run{
                                //正式进入下载状态
                                withContext(Dispatchers.Main){
                                    updateTaskStatus(
                                        context = context,
                                        downloadTask.taskInformation.taskID,
                                        TaskStatus.Activating
                                    )
                                }

                            }

                            Log.d("taskInfo","Starting multi-thread download: currentTask: $downloadTask")


                        }

                        else{
                            chunksRangeList = downloadTask.taskInformation.chunksRangeList
                            chunkDownloadedList = downloadTask.chunkProgress.toMutableList()
                        }

                        taskJobBuilder(
                            context = context,
                            downloadTask = downloadTask,
                            chunkRangeList = chunksRangeList,
                            chunkDownloadedList = chunkDownloadedList
                        ).join()
                    }

                    catch (e: Exception) {
                        println("taskInfo: ${URL(downloadTask.taskInformation.downloadUrl)}, error: ${e.toString()}")
                        //以后还要传递 错误信息给任务栏上

                        withContext(Dispatchers.Main){
                            updateTaskStatus(
                                context = context,
                                downloadTask.taskInformation.taskID,
                                TaskStatus.Stopped
                            )
                        }


                    }

                }


                jobs.add(job)

            }

            else{
                Log.d("taskInfo","the task: ${downloadTask.taskInformation.fileName} is already exists!")
            }
        }



    }

    //只负责 job 的创建
    fun taskJobBuilder(
        context: Context,
        downloadTask: DownloadTask,
        chunkRangeList: List<Pair<Long, Long>>,
        chunkDownloadedList: List<Int>,
    ): Job = scope.launch {

        Log.d("taskInfo","chunkInformation: $chunkRangeList \n $chunkDownloadedList")

        val currentChunkProgress = chunkDownloadedList.toMutableList()

        val currentTaskStatusFlow: StateFlow<TaskStatus?>? = downloadingTaskFlow
            .map {
                it.firstOrNull { it.taskInformation.taskID == downloadTask.taskInformation.taskID }?.taskStatus
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = TaskStatus.Pending
            )

        //监听UI
        val currentSpeedLimitFlow: StateFlow<Long>? = downloadingTaskFlow
            .map {
                it.firstOrNull { it.taskInformation.taskID == downloadTask.taskInformation.taskID }?.speedLimit ?: 0L
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = 0L
            )

        val activeTaskCountState = MutableStateFlow<Int> ( currentChunkProgress.size )

        //waiting for the first timer update
//        val currentDelayDurationState = MutableStateFlow<Float>( 250F )

        val updateSpeedTimer = Timer()
        var timerCount = 0

        val updateTask = object : TimerTask(){
            override fun run(){

                timerCount+=1

                val timerCoroutineScope = CoroutineScope(Dispatchers.IO)

                if(
                    currentTaskStatusFlow?.value == TaskStatus.Paused ||
                    currentTaskStatusFlow?.value == TaskStatus.Pending
                ) return

                if(currentTaskStatusFlow?.value == TaskStatus.Stopped){
                    updateSpeedTimer.cancel()
                }

                var recordSize = downloadingTaskFlow.value.find {
                    it.taskInformation.taskID == downloadTask.taskInformation.taskID
                }?.chunkProgress?.sum() ?: 0

                //250ms update
                val speed = ((currentChunkProgress.sum() - recordSize)*4).toLong()
//                val activeChunk = currentChunkProgress.size - currentChunkProgress.count { it == chunkFinished }
                activeTaskCountState.value = currentChunkProgress.size - currentChunkProgress.count { it == chunkFinished }

//                //每个线程的 DelayDuration
//                currentDelayDurationState.value =
//                    if(currentSpeedLimitFlow?.value == 0L || speed == 0L){ 0.0F }
//                    else{
//                        (currentSpeedLimitFlow?.value!!)/(speed).toFloat()* //per ms delay
//                         activeChunk // per chunk delay
//
//                    }

                updateTaskInformation(
                    taskID = downloadTask.taskInformation.taskID,
                ){
                    it.copy(
                        chunkProgress = currentChunkProgress.toList(),
                        currentSpeed = speed,
                    )
                }.run{
//                    Log.d("taskInfo","speed: $speed / delay ${currentDelayDurationState.value} ms / speedLimitFlow:${currentSpeedLimitFlow?.value}" )
                    if(timerCount % 4 == 0){

                        val newTaskInformation = findTask(downloadTask.taskInformation.taskID)

                        timerCoroutineScope.launch {
                            withContext(Dispatchers.IO){
                                MyDataStore.updateTask(
                                    context = context,
                                    taskID = downloadTask.taskInformation.taskID,
                                    newTaskInformation = newTaskInformation
                                ).run {
                                    Log.d("taskInfo","timer update storage: $newTaskInformation")
                                }

                            }

                        }


                    }
                }

            }
        }

        withContext(Dispatchers.Main) {
            updateTaskInformation(
                taskID = downloadTask.taskInformation.taskID,
            ){
                it.copy(
                    taskStatus = TaskStatus.Pending,
                    message = "正在创建线程..."
                )
            }

        }

        updateSpeedTimer.schedule(updateTask, 1000, 250)

        val activeTaskCount = AtomicInteger(chunkDownloadedList.size)
        val chunkJobs = chunkRangeList.mapIndexed { chunkIndex, (start, end) ->

            if (chunkDownloadedList[chunkIndex] != chunkFinished) {

                launch {

                    downloadChunk(
                        context = context,
                        downloadTask = downloadTask,

                        // example: total: (0-300) storage:50(0-49) newRequest => 50-300
                        // rangeStart = defaultStart + chunkDownloaded
                        rangeStart =
                            if(chunkDownloadedList.all{bytes -> bytes == 0}) start
                            else start + chunkDownloadedList[chunkIndex],
                        rangeEnd = end,

                        chunkIndex = chunkIndex,
                        taskActiveState = currentTaskStatusFlow,
//                        currentDelayDurationState = currentDelayDurationState,
                        speedLimitFlow = currentSpeedLimitFlow,
                        activeTaskCountState = activeTaskCountState,
                        downloadCallback = { current, total ->

                            //taskProgress update Area
                            // 1. 35 + 0
                            // 2. 35 + 350
                            currentChunkProgress[chunkIndex] = current.toInt() + chunkDownloadedList[chunkIndex]

                            if (current >= total) {
                                activeTaskCount.decrementAndGet()
                                //已完成标志
                                currentChunkProgress[chunkIndex] = chunkFinished
                                Log.d("taskInfo","chunkIndex:$chunkIndex completed. residual Task: $activeTaskCount")

                                //问题 当最后一个 chunk completed 的时候 目前的数据如下: [-1, -1, 2201480, -1, -1, -1]
                                //说明。。流有延迟 怎么办? 那就干脆整一个外部的count吧
                                if (activeTaskCount.value == 0) {

                                    Log.d(
                                        "taskInfo",
                                        "${downloadTask.taskInformation.fileName} all chunks completed."
                                    )

                                    scope.launch {

                                        withContext(Dispatchers.Main) {

                                            updateTaskProgress(
                                                downloadTask.taskInformation.taskID,
                                                chunkProgressList = MutableList<Int>(chunkRangeList.size){chunkFinished},
                                                currentSpeed = 0L,
                                            )

                                            updateTaskStatus(
                                                context = context,
                                                downloadTask.taskInformation.taskID,
                                                TaskStatus.Finished
                                            )

                                            removeTask(
                                                context = context,
                                                taskID = downloadTask.taskInformation.taskID,
                                                isCompleteAutoRemove = true,
                                            )
                                        }

                                        updateSpeedTimer.cancel()


                                    }


                                }

                            }


                        }
                    )


                }

            }

            else{
                activeTaskCount.decrementAndGet()
                //just empty
                launch {  }
            }


        }

        chunkJobs.joinAll()

    }

    fun preallocateSpace(context: Context, uri: Uri, targetSize: Long) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use { fos ->
                    // 移动到目标位置-1（因为写入1字节会自动扩展）
                    fos.channel.position(targetSize - 1).use {
                        fos.write(0) // 写入单个空字节
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("PreAlloc", "空间预分配失败：${e.message}")
        }
    }

     private inline fun updateTaskInformation(
        taskID: String,
        crossinline propTransform: (DownloadTask) -> DownloadTask
    ): DownloadTask {

         var latestDownloadTask: DownloadTask = findTask(taskID) ?: DownloadTask.Default

         if(latestDownloadTask.taskStatus == TaskStatus.Finished){
             finishedTaskState.update {
                 it.map{ task ->
                     if(task.taskInformation.taskID == taskID){
                         latestDownloadTask = propTransform(task)
                         latestDownloadTask
                     }

                     else{
                         task
                     }
                 }
             }
         }

         else{
             downloadingTaskState.update {
                 it.map{ task ->
                     if(task.taskInformation.taskID == taskID){
                         latestDownloadTask = propTransform(task)
                         latestDownloadTask
                     }

                     else{
                         task
                     }
                 }
             }
         }

         return latestDownloadTask

    }

    private fun updateTaskProgress(
        taskID: String,
        chunkProgressList: MutableList<Int>,
        currentSpeed: Long,
    ) = updateTaskInformation(taskID) {
//        Log.d("taskInfo","chunkProgressList:${chunkProgressList.sum()}")
        it.copy(
            chunkProgress = chunkProgressList.toList(),
            currentSpeed = currentSpeed
        )
    }

    fun updateTaskSpeedLimit(
        taskID: String,
        speedLimit: Long,
    ) = updateTaskInformation(taskID) { it.copy(speedLimit = speedLimit) }

    fun updatefileName(
        taskID: String,
        fileName: String,
    ) = updateTaskInformation(taskID) { it.copy(
        taskInformation =
            it.taskInformation.copy(fileName = fileName)
    )}

     suspend fun updateTaskStatus(
         context: Context,
         taskID: String,
         taskStatus: TaskStatus,
    ){

         var newTaskInformation: DownloadTask? = null

         updateTaskInformation(taskID) {
             newTaskInformation = it.copy(taskStatus = taskStatus)
             it.copy(taskStatus = taskStatus)
        }

         MyDataStore.updateTask(
             context = context,
             taskID = taskID,
             newTaskInformation = newTaskInformation
         ).run {
             Log.d("taskInfo","storage Info: $newTaskInformation")
         }

        Log.d("taskStatus", "$taskID New task status: ${downloadingTaskFlow.value.find { it.taskInformation.taskID == taskID }?.taskStatus}")

    }


    ///这里有两个情景
    /// 1. 下载中 => 已完成 的删除 此处应只删除UI 不涉及数据
    /// 2. 其余均为涉及到数据的删除
    suspend fun removeTask(
        context: Context,
        taskID: String?,
        isCompleteAutoRemove: Boolean = false,
    ){

        var selectedTask: DownloadTask? = null

        if(isCompleteAutoRemove){
            selectedTask = downloadingTaskFlow.value.find { task -> task.taskInformation.taskID == taskID }
        }

        else{
            selectedTask = downloadingTaskFlow.value.find { task -> task.taskInformation.taskID == taskID }

            if(selectedTask == null) {
                selectedTask = finishedTaskFlow.value.find { task -> task.taskInformation.taskID == taskID }
            }

        }

        Log.d("taskInfo","try remove ${selectedTask?.taskInformation?.taskID}")


        selectedTask?.let {

            if(isCompleteAutoRemove){
                downloadingTaskState.value =
                    downloadingTaskState.value - selectedTask

                Log.d("taskInfo","download UI delete")

                finishedTaskState.value =
                    finishedTaskState.value + selectedTask
            }

            else{

                if(selectedTask.taskStatus == TaskStatus.Activating){
                    //需等待 非Active 流程 才可移除
                    withContext(Dispatchers.Main){
                        updateTaskStatus(
                            context = context,
                            selectedTask.taskInformation.taskID,
                            TaskStatus.Paused
                        )
                    }
                }

                else{

                    //? 直接在UI上清除 不知道会不会有什么副作用?
                    downloadingTaskState.value =
                        downloadingTaskState.value - selectedTask

                    finishedTaskState.value =
                        finishedTaskState.value - selectedTask

                }

                scope.launch{
                    cancelJob(selectedTask.taskInformation.taskID)
                    MyDataStore.removeTask(context = context,taskID = taskID)
                    Log.d("taskInfo","download storage delete")
                }
            }


        }
    }

    fun removeTasks(context: Context){
        for(currentTask in DownloadViewModel.MultiChooseSet){
            scope.launch {
                removeTask(
                    context = context,
                    taskID = currentTask,
                )
            }
        }

    }

    fun removeAllDownloadingTasks(context: Context){
        downloadingTaskState.value = emptyList()

        scope.launch{
            cancelAll()
            MyDataStore.removeAllTasks(context = context)
            Log.d("taskInfo","remove All Downloading tasks")
        }
    }

    suspend fun getFileInformation(
        downloadUrl: String,
    ): TaskInformation{

        var taskInformation = TaskInformation.Default

        withContext(Dispatchers.IO) {

            try{

                val headResponse = downloadRequest.getCustomHead(downloadUrl)

                lateinit var headers: Headers

                if(headResponse.isSuccessful){ headers = headResponse.headers() }

                else{
                    Log.d("taskInfo","headResponse Error: triggering fallback contentGET")
                    val getResponse = downloadRequest.getCustomUrl(downloadUrl,"bytes=0-1")
                    if(getResponse.isSuccessful){ headers = getResponse.headers() }
                }

                // content-disposition: attachment; filename=banguLite-0.5.8-armeabi-v7a.apk
                // content-length: 123456
                taskInformation = taskInformation.copy(
                    downloadUrl = downloadUrl,
                    taskID = downloadUrl.hashCode().toString(),
                    fileSize = headers["content-length"]?.toLongOrNull() ?: 0L,
                    fileName = headers["content-disposition"]?.toString()?.split("filename=")?.last() ?: taskInformation.fileName,

                )

            }

            catch (e: Exception) {
                Log.d("taskInfo", e.toString())
            }
        }

        Log.d("taskInfo","fileInformation: $taskInformation")

        return taskInformation


    }

    // 获取文件大小
     suspend fun getFileSize(
        downloadUrl: String,
        chunkRequestFallback: () -> Long? = { emptyLength }): Long? {

        var contentLength = emptyLength

        withContext(Dispatchers.IO) {

            try{

                Log.d("taskInfo","try make head response")
                val response = downloadRequest.getCustomHead(downloadUrl)
                Log.d("taskInfo","head response done.")


                if(response.isSuccessful){
                    val headers = response.headers()

                    Log.d("headers","headers Info:$headers")

                    contentLength = headers["content-length"]?.toLongOrNull()
                    Log.d("taskInfo","contentLength:$contentLength")


                }

                if(contentLength == null){
                    Log.d("taskInfo","fail for unknown reason")
                    contentLength = chunkRequestFallback()
                }


            }

            catch (e: Exception) {
                Log.d("taskInfo", e.toString())

            }
        }

        return contentLength

    }

     fun calculateChunks(fileSize: Long, chunkCount: Int): List<Pair<Long, Long>> {
        val chunks = mutableListOf<Pair<Long, Long>>()
        var start = 0L

        //5192:5 => 1038..2

        val chunkSize = fileSize/chunkCount

        while (start < fileSize) {
            val end = (start + chunkSize - 1).coerceAtMost(fileSize - 1)
            chunks.add(start to end)
            start += chunkSize
        }
        return chunks
    }


    // 下载单个块
    private suspend fun downloadChunk(
        context: Context,
        downloadTask: DownloadTask,
        rangeStart: Long, rangeEnd: Long, chunkIndex: Int,
        taskActiveState: StateFlow<TaskStatus?>? = null,
//        currentDelayDurationState: StateFlow<Float>? = null,
        activeTaskCountState: StateFlow<Int>? = null,
        speedLimitFlow: StateFlow<Long>? = null,
        downloadCallback: ProgressCallback = { current,total -> },

    ) {

        var chunkFailedCount = 0
        val maxRetries = 3

        Log.d("taskInfo","chunkIndex: $chunkIndex : [$rangeStart/$rangeEnd]")

        val buffer = ByteArray(8 * 1024)
        val totalLength = (rangeEnd - rangeStart)+1
        var totalBytesRead = 0L

        //重试链接 直到任一chunk达到3次次数 直接置为 stop 然后 停止并CancelJob

        while( chunkFailedCount <= maxRetries ){
            try{
                val response = downloadRequest.getCustomUrl(downloadTask.taskInformation.downloadUrl,"bytes=${rangeStart+totalBytesRead}-$rangeEnd")

                // 检查响应是否成功
                if (!response.isSuccessful) {
                    if(totalBytesRead == totalLength) return
                    //奇怪问题 当下载完毕的时候 会抛出 501 附带 chunkIndex:1 failedAt: 1287085/1287085
                    //是的 下载完成之后的501错误 我不明白这是怎么回事
                    throw Exception("Download failed with : ${response.code()}, ${response.message()}")
                }

                if (TaskStatus.Pending == taskActiveState?.value) {
                    withContext(Dispatchers.Main) {
                        updateTaskInformation(
                            taskID = downloadTask.taskInformation.taskID,
                        ){
                            it.copy(
                                taskStatus = TaskStatus.Activating,
                                message = null
                            )
                        }
                    }
                }

                var periodicBytesRead = 0L
                var startTime = Instant.now().toEpochMilli()

                // 根据目标速度计算应该花费的时间（毫秒）
                var expectedTimeMs = 0L

                response.body()?.let{
                    it.byteStream().use{ input ->
                        //SAF模式之下 只能依靠 contentResolver.openFileDescriptor 来 提供RAF模式的 写入
                        context.contentResolver.openFileDescriptor(Uri.parse(downloadTask.taskInformation.storagePath), "rw")?.use { pfd ->

                            FileOutputStream(pfd.fileDescriptor).use { fos ->
                                fos.channel.use { channel ->
                                    //严格来说 在这个作用域之下 才能算是之前的RAF模式
                                    channel.position(rangeStart) // 初始定位到分块起始位置 相当于 .seek() 的作用

                                    var currentBytesRead: Int
                                    while (input.read(buffer).also { currentBytesRead = it } != chunkFinished) {
                                        fos.write(buffer, 0, currentBytesRead) // 自动更新文件指针
                                        totalBytesRead += currentBytesRead

                                        speedLimitFlow?.let{

                                            if(speedLimitFlow.value == 0L) return@let

                                            val elapsedTimeMs = Instant.now().toEpochMilli() - startTime
                                            val limitBytesPerChunk = speedLimitFlow.value / activeTaskCountState!!.value //per chunk

                                            periodicBytesRead += currentBytesRead

                                            // example : speed 300KB target 50KB
                                            // 300KB / (1)chunk / 1000 => 1ms => 0.3KB/ms
                                            // 50KB / 0.3KB/ms => 166.7 ms
                                            // => periodicBytesRead / (activeTaskCountState / 1000) => resultExpectedMs
                                            expectedTimeMs = (periodicBytesRead*1000) / limitBytesPerChunk

                                            // 如果实际时间小于期望时间，需要延迟
                                            if (elapsedTimeMs < expectedTimeMs) {
                                                //等待到 expectedTimeMs 为止
                                                val delayTimeMs = expectedTimeMs - elapsedTimeMs
                                                //Log.d("taskInfo", "chunkIndex: $chunkIndex : delay $delayTimeMs ms | speedLimit: ${speedLimitFlow.value} | elapsedTime: $elapsedTimeMs | expectedTime: $expectedTimeMs")
                                                delay(delayTimeMs)
                                            }

                                            // 定期重置计时器（不管是否延迟）
                                            if (periodicBytesRead > limitBytesPerChunk || elapsedTimeMs > 1000) {
                                                startTime = Instant.now().toEpochMilli()
                                                periodicBytesRead = 0L
                                            }


                                        }


                                        if (TaskStatus.Paused == taskActiveState?.value) {
                                            Log.d("taskStatus", "[${taskActiveState.value}] $chunkIndex triggered hang up")

                                            taskActiveState.first { TaskStatus.Activating == it } //hangup : State wait for the update

                                            Log.d("taskStatus", "$chunkIndex triggered resume")

                                        }

                                        downloadCallback(totalBytesRead, totalLength)
                                    }
                                }
                            }

                        }
                    }
                }

                Log.d("TaskStatus","$rangeStart-$rangeEnd writeFinished")

            }

            catch(e: Exception) {

                chunkFailedCount+=1

                if(chunkFailedCount == maxRetries){

                    updateTaskInformation(
                        taskID = downloadTask.taskInformation.taskID,
                    ){
                        it.copy(
                            taskStatus = TaskStatus.Stopped,
                            message = e.toString()
                        )
                    }

                    Log.d("taskInfo","[${downloadTask.taskInformation.fileName}] failed.")
                    cancelJob(downloadTask.taskInformation.taskID)
                }

                else{

                    Log.d("taskInfo","[${downloadTask.taskInformation.fileName}] chunkIndex:$chunkIndex failedAt: $totalBytesRead/$totalLength \n failed reason: ${e.toString()} try resumed it After ${chunkFailedCount}s ")
                    delay(1000*chunkFailedCount.toLong())
                }


                //以后还要传递 错误信息给任务栏上?

            }

        }

    }

    fun findTask(
        taskID: String
    ): DownloadTask? {
        return downloadingTaskFlow.value.find{ it.taskInformation.taskID == taskID } ?:
               finishedTaskFlow.value.find{ it.taskInformation.taskID == taskID }
    }

    // 等待所有任务完成
    suspend fun waitForAll() {
        jobs.joinAll()
    }

    fun cancelJob(
        coroutineName: String
    ){
        jobs.find {
            (it as? CoroutineScope)?.coroutineContext?.get(CoroutineName)?.name == coroutineName
        }?.cancel()
    }

    // 取消所有任务
    fun cancelAll() {
        jobs.forEach { it.cancel() }
    }




}



