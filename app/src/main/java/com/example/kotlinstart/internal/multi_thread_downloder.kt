package com.example.kotlinstart.internal

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*


import java.io.FileOutputStream
import java.io.IOException

import java.net.URL
import kotlin.coroutines.coroutineContext

import kotlin.random.Random
import kotlin.text.toLongOrNull

typealias ProgressCallback = (current: Long, total: Long) -> Unit
const val chunkFinished = -1
val emptyLength: Long? = null


object MultiThreadDownloadManager: ViewModel() {

    val retrofitClient = HttpRequestClient.githubClient
    private val downloadRequest = retrofitClient.create(DownloadApi::class.java)

    private val scope = CoroutineScope(Dispatchers.IO)
    private val jobs = mutableListOf<Job>()

    private val downloadingTaskState = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadingTaskFlow: StateFlow<List<DownloadTask>> = downloadingTaskState.asStateFlow()

    private val finishedTaskState = MutableStateFlow<List<DownloadTask>>(emptyList())
    val finishedTaskFlow = finishedTaskState.asStateFlow()

    fun init(context: Context){
        scope.launch{

            val flow = MyDataStore.getAllTasks(context)

            //重置所有的 Activating 为 Paused
            flow.collect{ list ->
                downloadingTaskState.value = list.map {
                    if(it.taskStatus == TaskStatus.Activating){
                        it.copy(
                            taskStatus = TaskStatus.Paused
                        )
                    }

                    else{
                        it
                    }
                }
            }

        }
    }

    // 添加下载任务
    @OptIn(DelicateCoroutinesApi::class)
    fun addTask(
        context: Context,
        downloadTask: DownloadTask?,
        chunkCount: Int = 5,
        isResume: Boolean = false
    ) {

        downloadTask?.let{ downloadTask ->
            //不存在任务时添加
            // wired: it[CoroutineName]?.name null /
            if(!jobs.any{
                //outside name
                val jobName = (it as? CoroutineScope)?.coroutineContext?.get(CoroutineName)?.name
                jobName == downloadTask.taskInformation.downloadUrl.hashCode().toString()
            }){

                val job = CoroutineScope(
                    Dispatchers.IO +
                    CoroutineName(downloadTask.taskInformation.downloadUrl.hashCode().toString())
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
                            if(downloadTask.fileSize != 0L){
                                fileSize = downloadTask.fileSize
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
                                ) }.await() ?: return@launch

                                //如果获取不到信息 暂且先直接退出
                            }

                            Log.d("taskInfo","start allocChunks")

                            //UI任务显示: 初始化
                            downloadingTaskState.value = downloadingTaskFlow.value + downloadTask

                            chunksRangeList = calculateChunks(fileSize, chunkCount)
                            chunkDownloadedList = MutableList<Int>(chunksRangeList.size){0}

                            Log.d("taskInfo","chunkList: $chunksRangeList")


                            MyDataStore.addTask(
                                context = context,
                                newTask = downloadTask.copy(
                                    taskInformation = downloadTask.taskInformation.copy(
                                        chunkCount = chunkCount
                                    ),
                                    chunkProgress = chunkDownloadedList,
                                    fileSize = fileSize,
                                    taskStatus = TaskStatus.Activating
                                )
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
                            chunksRangeList = calculateChunks(downloadTask.fileSize, downloadTask.taskInformation.chunkCount)
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
                Log.d("taskInfo","the task: ${downloadTask.taskInformation.taskName} is already exists!")
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

            try {

                Log.d("taskInfo","chunkInformation: $chunkRangeList \n $chunkDownloadedList")

                //jesus..?

                val currentTaskStatusFlow: StateFlow<TaskStatus?>? = downloadingTaskFlow
                    .map {
                        it.firstOrNull { it.taskInformation.taskID == downloadTask.taskInformation.taskID }?.taskStatus
                    }
                    .stateIn(
                        scope = scope,
                        started = SharingStarted.Eagerly, //当下载开始时 (downloadChunk回调) 开始监听
                        initialValue = TaskStatus.Pending
                    )

                val currentChunkProgress = chunkDownloadedList.toMutableList()

                withContext(Dispatchers.Main) {
                    updateTaskStatus(
                        context = context,
                        downloadTask.taskInformation.taskID,
                        TaskStatus.Activating
                    )
                }


                val chunkJobs = chunkRangeList.mapIndexed { chunkIndex, (start, end) ->
                    if (chunkDownloadedList[chunkIndex] != chunkFinished) {

                        launch {

                            //实际下载的进度正确 但是进度条却不知道自己的数据出现偏差 怎么办?
                            //那就自己额外添加sum的进度过去咯。。
                            //除非。。我主动请求流的数据偏移? 这有可能吗???

                            downloadChunk(
                                context = context,
                                downloadUrl = downloadTask.taskInformation.downloadUrl,
                                destination = Uri.parse(downloadTask.taskInformation.storagePath),
                                rangeStart =
                                    if(chunkDownloadedList.isEmpty()) start
                                    else start + chunkDownloadedList[chunkIndex],
                                rangeEnd = end,
                                chunkIndex = chunkIndex,
                                taskActiveState = currentTaskStatusFlow,
                                downloadCallback = { current, total ->

                                    //taskProgress update Area
                                    currentChunkProgress[chunkIndex] = current.toInt()

                                    //理想:只有一个thread在执行 UI更新行为 不过那样不就是Timer了??


                                    var recordSize = downloadingTaskFlow.value.find {
                                        it.taskInformation.taskID == downloadTask.taskInformation.taskID
                                    }?.chunkProgress?.sum() ?: 0

                                    val speed = ((currentChunkProgress.sum() - recordSize)).toLong()

                                    updateTaskProgress(
                                        taskID = downloadTask.taskInformation.taskID,
                                        chunkProgressList = currentChunkProgress,
                                        currentSpeed = speed
                                    )


                                    //chunk/task finish area

                                    //有时候就是会出现 3068581/3068580 => 1.0000004 这样的情况 我也不知道为什么
                                    if (current >= total) {
                                        //已完成标志
                                        currentChunkProgress[chunkIndex] = chunkFinished
                                        println("${downloadTask.taskInformation.taskName} chunkIndex:$chunkIndex completed.")

                                        if (true == downloadingTaskFlow.value.find {
                                                it.taskInformation.taskID == downloadTask.taskInformation.taskID
                                            }?.chunkProgress?.all { it == chunkFinished }) {

                                            Log.d(
                                                "taskInfo",
                                                "${downloadTask.taskInformation.taskName}  all chunks completed."
                                            )

                                            scope.launch {

                                                withContext(Dispatchers.Main) {
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


                                            }


                                        }


                                    }


                                }
                            )


                        }

                    }

                    else{
                        //just empty
                        launch {  }
                    }


                }

                chunkJobs.joinAll()

            }

            catch(e: Exception) {
                println("taskInfo: ${URL(downloadTask.taskInformation.downloadUrl)}, error: ${e.toString()}")
                //以后还要传递 错误信息给任务栏上

            }

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
    ){
        downloadingTaskState.value = downloadingTaskState.value.map { task ->

        if(task.taskInformation.taskID == taskID){
            propTransform(task)
        }

        else{
            task
        }
    }



    }

    private fun updateTaskProgress(
        taskID: String,
        chunkProgressList: MutableList<Int>,
        currentSpeed: Long,
    ){
        updateTaskInformation(taskID) {
            it.copy(
                chunkProgress = chunkProgressList.toList(),
                currentSpeed = currentSpeed
            )
        }

    }

    fun updateTaskSpeedLimit(
        taskID: String,
        speedLimit: Long,
    ) = updateTaskInformation(taskID) { it.copy(speedLimit = speedLimit) }

     suspend fun updateTaskStatus(
         context: Context,
         taskID: String,
         taskStatus: TaskStatus,
    ){

         lateinit var newTaskInformation: DownloadTask

         updateTaskInformation(taskID) {
             newTaskInformation = it.copy(taskStatus = taskStatus)
             it.copy(taskStatus = taskStatus)
        }

         MyDataStore.updateTask(
             context = context,
             taskID = newTaskInformation.taskInformation.taskID,
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

                    withContext(Dispatchers.Main){
                        updateTaskStatus(
                            context = context,
                            selectedTask.taskInformation.taskID,
                            TaskStatus.Paused
                        )
                    }
                }

                else{

                    //? 直接清除 不知道会不会有什么副作用
                    downloadingTaskState.value =
                        downloadingTaskState.value - selectedTask

                    finishedTaskState.value =
                        finishedTaskState.value - selectedTask

                    scope.launch{
                        MyDataStore.removeTask(context = context,taskID = taskID)
                        Log.d("taskInfo","download storage delete")
                    }
                }


            }



            //需等待 非Active 流程 才可移除



        }
    }



    fun removeAllTasks(context: Context){
        downloadingTaskState.value = emptyList()

        scope.launch{
            MyDataStore.removeAllTasks(context = context)
            Log.d("taskInfo","remove All Downloading tasks")
        }
    }

    // 获取文件大小
    private suspend fun getFileSize(
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

//                    Log.d("taskInfo","HEAD Request Info:$headers")

                    contentLength = headers["content-length"]?.toLongOrNull()
                    Log.d("taskInfo","contentLength:$contentLength")

                    if(contentLength == null){
                        Log.d("taskInfo","fail for unknown reason")
                        contentLength = chunkRequestFallback()
                    }
                }

            }

            catch (e: Exception) {
                Log.d("taskInfo", e.toString())

            }
        }

        return contentLength

    }

    private fun calculateChunks(fileSize: Long, chunkCount: Int): List<Pair<Long, Long>> {
        val chunks = mutableListOf<Pair<Long, Long>>()
        var start = 0L

        //5192 5 => 1038..2

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
        downloadUrl: String,
        destination: Uri = Uri.parse(""),
        rangeStart: Long = 0L, rangeEnd: Long = 1L, chunkIndex: Int = 0,
        taskActiveState: StateFlow<TaskStatus?>? = null,
        downloadCallback: ProgressCallback = { current,total -> },

    ) {

        val response = downloadRequest.getCustomUrl(downloadUrl,"bytes=$rangeStart-$rangeEnd")

        Log.d("taskInfo","$chunkIndex : [$rangeStart/$rangeEnd]")

        val buffer = ByteArray(8 * 1024)
        val totalLength = rangeEnd - rangeStart
        var totalBytesRead = 0L

        response.body()?.let{
            it.byteStream().use{ input ->
                context.contentResolver.openFileDescriptor(destination, "rw")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { fos ->
                        fos.channel.use { channel ->
                            channel.position(rangeStart) // 初始定位到分块起始位置

                            var currentBytesRead: Int
                            while (input.read(buffer).also { currentBytesRead = it } != chunkFinished) {
                                fos.write(buffer, 0, currentBytesRead) // 自动更新文件指针
                                totalBytesRead += currentBytesRead


                                // 模拟延迟和回调
                                delay((Random.nextFloat()*1500).toLong())
//                                delay((Random.nextFloat()*500).toLong())

                                if (TaskStatus.Paused == taskActiveState?.value) {
                                    Log.d("taskStatus", "$chunkIndex triggered hang up")

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



    // 等待所有任务完成
    suspend fun waitForAll() {
        jobs.joinAll()
    }

    // 取消所有任务
    fun cancelAll() {
        jobs.forEach { it.cancel() }
    }




}

enum class BinaryType(
    val binaryType: String,
    val size: Long
){
    B("B",0),
    KB("KB",1*1024),
    MB("MB",1*1024*1024),
    GB("GB",1*1024*1024*1024)
}

fun convertBinaryType(value: Long): String{
    var resultType = BinaryType.B.binaryType
    var resultValue = value.toFloat()

    BinaryType.entries.any {
        //1000*1024 => 1000 KB => 0.97MB

        if(resultValue<1024) {
            //划分之后 如果数值超过 1000 那么应该再切割一下 并更新Type
            resultType = it.binaryType

            if(resultValue>=1000){
                resultValue /= BinaryType.KB.size
                return@any false
            }

            else{
                return@any true
            }

        }

        else{
            resultValue /= BinaryType.KB.size
        }

        return@any false



    }


    var splitResult = resultValue.toString().split(".")

    if(splitResult.last().length <= 2){
        return "$resultValue$resultType"
    }

    return "${splitResult[0]}.${splitResult[1].substring(0,2)}$resultType"


}
