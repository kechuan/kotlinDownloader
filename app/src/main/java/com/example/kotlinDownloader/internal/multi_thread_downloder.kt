package com.example.kotlinDownloader.internal


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.kotlinDownloader.internal.android.TaskProgressBinderService
import com.example.kotlinDownloader.internal.android.defaultToaster
import com.example.kotlinDownloader.internal.android.requestNoticePermission
import com.example.kotlinDownloader.models.DownloadViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.Headers
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.time.Instant
import java.util.*

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
            if(it.taskStatus == TaskStatus.Activating) it.copy(taskStatus = TaskStatus.Paused)
            else it
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
    ): Boolean{

        downloadTask?.let{ downloadTask ->

            val taskID: String = downloadTask.taskInformation.taskID

            //任务重复添加检测

            //exist in TasksFlow
            if(findTask(taskID) != null){

                //exist in downloadFlow
                if(downloadingTaskFlow.value.find{ it.taskInformation.taskID == taskID } != null){
                    //自动处理为 Stop/Pause => resume 逻辑

                    //exist in Job
                    if(jobs.any{
                        //@Deprecated Job.coroutineContext[CoroutineName]?.name
                        val jobName = (it as CoroutineScope).coroutineContext[CoroutineName]?.name
                        jobName == downloadTask.taskInformation.taskID
                    }){
                        //被 updateTaskStatus Activity 接管
                        Log.d("taskInfo","the task: ${downloadTask.taskInformation.fileName} is already exists! resume it.")

                        scope.launch {
                            updateTaskStatus(
                                context = context,
                                taskID = taskID,
                                taskStatus = TaskStatus.Activating
                            )
                        }

                    }

                    else{

                        val job = CoroutineScope(
                            Dispatchers.IO +
                            CoroutineName(downloadTask.taskInformation.taskID)
                        ).launch {
                            //inside name
                            Log.d("taskInfo","job ScopeName name: ${coroutineContext[CoroutineName]?.name} / ${downloadTask.taskInformation.downloadUrl.hashCode()}")

                            try {
                                
                                var chunksRangeList: List<Pair<Long, Long>> = downloadTask.taskInformation.chunksRangeList
                                var chunkDownloadedList: MutableList<Int> = downloadTask.chunkProgress.toMutableList()
                                

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

                                return@launch


                            }

                        }


                        jobs.add(job)

                    }

                }

                //exist in finishedFlow
                else{
                    // 否则询问是否要重新下载
                    // Dialog提问 是否需要重新下载 如实际确认则为 删除finishedFlow的存在 然后重新执行这个函数
                    return@addTask false
                }

            }

            else{

                downloadingTaskState.value = downloadingTaskFlow.value + downloadTask

                val job = CoroutineScope(
                    Dispatchers.IO +
                    CoroutineName(downloadTask.taskInformation.taskID)
                ).launch {
                    //inside name
                    Log.d("taskInfo","job ScopeName name: ${coroutineContext[CoroutineName]?.name} / ${downloadTask.taskInformation.downloadUrl.hashCode()}")

                    try {

                        var fileSize = emptyLength

                        //没有三目表达式的情况之下 真的很不喜欢把 if/else 写在一个变量定义里
                        //假如其已经在dialog里获取到信息就直接沿用它的信息
                        if( downloadTask.taskInformation.fileSize != emptyLength){
                            fileSize = downloadTask.taskInformation.fileSize
                        }

                        else{



                            fileSize = async {
                                getFileInformation(downloadTask.taskInformation.downloadUrl).fileSize
                            }.await().also {

                                Log.d("taskInfo","fileName : ${downloadTask.taskInformation.fileName} contentLength Length: $it.")

                                if(it == emptyLength) {
                                    Log.d("taskInfo","contentLength get failed. cancel Task.")
                                    cancelJob(downloadTask.taskInformation.taskID)
                                    defaultToaster(context,"已取消任务: 无法获取文件信息")
                                    return@launch
                                }
                            }

                        }

                        var chunksRangeList: List<Pair<Long, Long>> = calculateChunks(fileSize, threadCount)
                        var chunkDownloadedList: MutableList<Int>  = MutableList<Int>(chunksRangeList.size){0}

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

                        MyDataStore.addNewTask(
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

                                //发送通知
                                if(requestNoticePermission(context)){
                                    TaskProgressBinderService.taskProgressBinder.startForeground()
                                }


                            }

                        }

                        Log.d("taskInfo","Starting multi-thread download: currentTask: $downloadTask")

                        

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

                        return@launch


                    }

                }


                jobs.add(job)
            }

        }

        return true

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
                it.firstOrNull { it.taskInformation.taskID == downloadTask.taskInformation.taskID }?.speedLimit ?: emptyLength
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = emptyLength
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

                //250ms update 影响 实际download的回调限速问题
                val speed = ((currentChunkProgress.sum() - recordSize)*4).toLong()
                activeTaskCountState.value = currentChunkProgress.size - currentChunkProgress.count { it == chunkFinished }

                //UI
                updateTaskProgress(
                    taskID = downloadTask.taskInformation.taskID,
                    chunkProgressList = currentChunkProgress,
                    currentSpeed = speed
                ).run{
                    //通知
                    TaskProgressBinderService.taskProgressBinder.updateTaskNotification(downloadTask.taskInformation.taskID)

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

        val chunkJobs = chunkRangeList.mapIndexed { chunkIndex, (start, end) ->

            if (chunkDownloadedList[chunkIndex] != chunkFinished) {

                launch {

                    downloadChunk(
                        context = context,
                        downloadTask = downloadTask,

                        // example: total: (0-300) storage:50(0-49) newRequest => 50-300
                        // rangeStart = defaultStart + chunkDownloaded
                        rangeStart = start + chunkDownloadedList[chunkIndex],
                        rangeEnd = end,

                        chunkIndex = chunkIndex,
                        taskActiveState = currentTaskStatusFlow,
                        speedLimitFlow = currentSpeedLimitFlow,
                        activeTaskCountState = activeTaskCountState,
                        downloadCallback = { current, total ->

                            //taskProgress update Area
                            // 1. 35 + 0
                            // 2. 35 + 350
                            currentChunkProgress[chunkIndex] = current.toInt() + chunkDownloadedList[chunkIndex]

                            if (current >= total) {

                                activeTaskCountState.value -= 1
//                                activeTaskCount.decrementAndGet()
                                //已完成标志
                                currentChunkProgress[chunkIndex] = chunkFinished
                                Log.d("taskInfo","chunkIndex:$chunkIndex completed. residual Task: ${activeTaskCountState.value}")

                                //问题 当最后一个 chunk completed 的时候 目前的数据如下: [-1, -1, 2201480, -1, -1, -1]
                                //说明。。流有延迟 怎么办? 那就干脆整一个外部的count吧
//                                if (activeTaskCount.value == 0) {
                                if (activeTaskCountState.value == 0) {

                                    Log.d(
                                        "taskInfo",
                                        "${downloadTask.taskInformation.fileName} all chunks completed."
                                    )

                                    finishTaskCallback(
                                        context,
                                        downloadTask
                                    ).run {
                                        updateSpeedTimer.cancel()
                                    }



                                }

                            }


                        }
                    )


                }

            }

            else{
                activeTaskCountState.value -= 1
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

                     else task

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

                     else task

                 }
             }
         }

         return latestDownloadTask

    }

    private fun updateTaskProgress(
        taskID: String,
        chunkProgressList: MutableList<Int>,
        currentSpeed: Long = emptyLength,
    ) = updateTaskInformation(taskID) {
        it.copy(
            chunkProgress = chunkProgressList.toList(),
            currentSpeed = currentSpeed
        )
    }

    fun updateTaskSpeedLimit(
        taskID: String,
        speedLimit: Long,
    ) = updateTaskInformation(taskID) { it.copy(speedLimit = speedLimit) }

    fun updateFileName(
        taskID: String,
        fileName: String,
    ) = updateTaskInformation(taskID) { it.copy(
        taskInformation = it.taskInformation.copy(fileName = fileName)
    )}

    //这里指代的是 所有 hangup 亦或者是 stop状态的Task

    fun pauseAllTask(context: Context){
        downloadingTaskFlow.value.filter {
            it.taskStatus == TaskStatus.Activating
        }.forEach { downloadTask ->
            updateTaskStatus(
                context,
                downloadTask.taskInformation.taskID,
                TaskStatus.Paused
            )
        }
    }

    fun resumeAllTask(context: Context){
        downloadingTaskFlow.value.filter {
            it.taskStatus == TaskStatus.Paused ||
            it.taskStatus == TaskStatus.Stopped
        }.forEach { downloadTask ->

            updateTaskStatus(
                context,
                downloadTask.taskInformation.taskID,
                TaskStatus.Activating
            )

        }
    }

    fun updateTaskStatus(
         context: Context,
         taskID: String,
         taskStatus: TaskStatus,
    ){

        updateTaskInformation(taskID) {
          var newTaskInformation: DownloadTask = it.copy(taskStatus = taskStatus)

          scope.launch{
              MyDataStore.updateTask(
                  context = context,
                  taskID = taskID,
                  newTaskInformation = newTaskInformation
              ).run {
                  Log.d("taskInfo","storage Info: $newTaskInformation")
                  Log.d("taskStatus", "$taskID New task status: ${downloadingTaskFlow.value.find { it.taskInformation.taskID == taskID }?.taskStatus}")
              }

          }

          newTaskInformation
      }.run{
        TaskProgressBinderService.taskProgressBinder.updateTaskNotification(taskID)
        TaskProgressBinderService.taskProgressBinder.updateSummaryNotification()
      }

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
        scope.launch {
            DownloadViewModel.MultiChooseTaskIDSet.forEach {
                removeTask(context = context, taskID = it)
            }
        }

    }

    fun removeAllTasks(context: Context){
        downloadingTaskState.value = emptyList()
        finishedTaskState.value = emptyList()

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

                lateinit var headers: Headers

                val headResponse = async { downloadRequest.getCustomHead(downloadUrl) }.await()
                if(headResponse.isSuccessful){ headers = headResponse.headers() }

                else{
                    Log.d("taskInfo","headResponse Error: triggering fallback contentGET")
                    val getResponse = async { downloadRequest.getCustomUrl(downloadUrl,"bytes=0-1") }.await()
                    if(getResponse.isSuccessful){ headers = getResponse.headers() }

                }

                // content-disposition: attachment; filename=banguLite-0.5.8-armeabi-v7a.apk
                // content-length: 123456
                taskInformation = taskInformation.copy(
                    downloadUrl = downloadUrl,
                    taskID = downloadUrl.hashCode().toString(),
                    fileSize = headers["content-length"]?.toLongOrNull() ?: emptyLength,
                    fileName = headers["content-disposition"]?.toString()?.split("filename=")?.last() ?: taskInformation.fileName,

                )

            }

            catch (networkError: Exception) {
                Log.d("taskInfo", networkError.toString())
                //message 抛出
            }
        }

        Log.d("taskInfo","fileInformation: $taskInformation")

        return taskInformation


    }

     fun calculateChunks(fileSize: Long, chunkCount: Int): List<Pair<Long, Long>> {
        val chunks = mutableListOf<Pair<Long, Long>>()
        var start = emptyLength

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
        activeTaskCountState: StateFlow<Int>? = null,
        speedLimitFlow: StateFlow<Long>? = null,
        downloadCallback: ProgressCallback = { current,total -> },

    ) {

        var chunkFailedCount = 0
        val maxRetries = 3

//        Log.d("taskInfo","chunkIndex: $chunkIndex : [$rangeStart/$rangeEnd]")

        val buffer = ByteArray(8 * 1024)
        val totalLength = (rangeEnd - rangeStart)+1
        var totalBytesRead = emptyLength

        //重试链接 直到任一chunk达到3次次数 直接置为 stop 然后 停止并CancelJob

        while( chunkFailedCount <= maxRetries ){
            try{
                val response = downloadRequest.getCustomUrl(downloadTask.taskInformation.downloadUrl,"bytes=${rangeStart+totalBytesRead}-$rangeEnd")

                // 检查响应是否成功
                if (!response.isSuccessful) {
                    if(totalBytesRead == totalLength) return
                    //奇怪问题 当下载完毕的时候 会抛出 501 附带 chunkIndex:1 failedAt: 1287085/1287085
                    //是的 下载完成之后的501错误 我不明白这是怎么回事
                    throw Exception("Download failed with: ${response.code()}, ${response.message()}")
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

                var periodicBytesRead = emptyLength
                var startTime = Instant.now().toEpochMilli()

                // 根据目标速度计算应该花费的时间（毫秒）
                var expectedTimeMs = emptyLength

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

//                                        Log.d("taskInfo","index:$chunkIndex $totalBytesRead / $totalLength")

                                        speedLimitFlow?.let{

                                            if(speedLimitFlow.value == emptyLength) return@let

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
                                                periodicBytesRead = emptyLength
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
                if(totalBytesRead == totalLength) return

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
                    Log.d(
                        "taskInfo",
                        "[${downloadTask.taskInformation.fileName}] chunkIndex:$chunkIndex failedAt: $totalBytesRead/$totalLength \n " +
                        "failed reason: ${e.toString()} try resumed it After ${chunkFailedCount}s "
                    )
                    delay(1000*chunkFailedCount.toLong())
                }

            }

        }

    }

    fun findTask(
        taskID: String?
    ): DownloadTask? {
        return downloadingTaskFlow.value.find{ it.taskInformation.taskID == taskID } ?:
               finishedTaskFlow.value.find{ it.taskInformation.taskID == taskID }
    }

    fun cancelJob(
        coroutineName: String
    ){
        jobs.find {
            (it as? CoroutineScope)?.coroutineContext?.get(CoroutineName)?.name == coroutineName
        }?.cancel()
    }

    // 取消所有任务
    fun cancelAll() = jobs.forEach { it.cancel() }

    fun finishTaskCallback(
        context: Context,
        downloadTask: DownloadTask
    ){

        updateTaskProgress(
            taskID = downloadTask.taskInformation.taskID,
            chunkProgressList = MutableList<Int>(downloadTask.chunkProgress.size){chunkFinished},
            currentSpeed = emptyLength,
        )

        updateTaskStatus(
            context = context,
            downloadTask.taskInformation.taskID,
            TaskStatus.Finished
        )

        scope.launch {
            removeTask(
                context = context,
                taskID = downloadTask.taskInformation.taskID,
                isCompleteAutoRemove = true,
            )
        }.run {

            // TODO 遥遥无期的重构
            // 为什么 taskStatus 声明后 还要在这里再插入一个 updateSummaryNotification?
            // 因为 updateTaskStatus 时 的 updateSummaryNotification 的流 还在 downloading里
            // 老实说已经开始吃亏了 吃为什么要特地造两条流的认知亏(当时不知道有filter这样的东西)
            TaskProgressBinderService.taskProgressBinder.updateSummaryNotification()
        }
    }




}

