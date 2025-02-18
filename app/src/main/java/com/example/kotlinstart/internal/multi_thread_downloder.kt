

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.kotlinstart.internal.DownloadTask
import com.example.kotlinstart.internal.TaskStatus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.launch

import java.net.URL
import java.net.HttpURLConnection
import kotlin.random.Random

typealias ProgressCallback = (current: Long, total: Long) -> Unit
const val chunkFinished = -1



object MultiThreadDownloadManager: ViewModel() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val jobs = mutableListOf<Job>()


    private val downloadingTaskState = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadingTaskFlow: StateFlow<List<DownloadTask>> = downloadingTaskState.asStateFlow()

    private val finishedTaskState = MutableStateFlow<List<DownloadTask>>(emptyList())
    val finishedTaskFlow = finishedTaskState.asStateFlow()

    suspend fun init(context: Context){
        scope.launch{

            val flow = MyDataStore.getAllTasks(context)

            flow.collect{ list ->
                downloadingTaskState.value = list
            }

        }
    }


    // 添加下载任务
    fun addTask(
        context: Context,
        downloadTask: DownloadTask,
        chunkCount: Int = 5,
    ) {

        //一个大热流 转 某个属性的热流
        val currentTaskStatusFlow: StateFlow<TaskStatus?>? = downloadingTaskFlow
            .map{
                it.firstOrNull{ it.taskInformation.taskID == downloadTask.taskInformation.taskID }?.taskStatus
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(), //当下载开始时 (downloadChunk回调) 开始监听
                initialValue = TaskStatus.Pending
            )

        val job = scope.launch {
            try {

                var fileSize = 0L

                //没有三目表达式的情况之下 真的很不喜欢把 if/else 写在一个变量定义里
                //假如其已经在dialog里获取到信息就直接沿用它的信息
                if(downloadTask.fileSize != 0L){
                    fileSize = downloadTask.fileSize
                }

                else{
                    fileSize = getFileSize(
                        downloadUrl = downloadTask.taskInformation.downloadUrl,
                        chunkRequestFallback = {
                            Log.d("taskInfo","trigger fallback contentGET")
                            scope.launch{
                                downloadChunk(
                                    context = context,
                                    downloadUrl = downloadTask.taskInformation.downloadUrl
                                )
                            }

                        }
                    ) ?: return@launch
                    //如果获取不到信息 暂且先直接退出
                }

                val chunks = calculateChunks(fileSize, chunkCount)

                //Int模式 利于保存
                val currentChunkProgress = MutableList<Int>(chunks.size){0}

                downloadTask.copy(
                    chunkProgress = currentChunkProgress,
                    fileSize = fileSize
                )

                //UI任务显示: 初始化
                downloadingTaskState.value = downloadingTaskFlow.value + downloadTask

                println("Starting multi-thread download: ${URL(downloadTask.taskInformation.downloadUrl)} chunks:$chunks currentTask: $downloadTask")

                MyDataStore.addTask(context = context,downloadTask)

                preallocateSpace(
                    context = context,
                    uri = Uri.parse(downloadTask.taskInformation.storagePath),
                    targetSize = fileSize
                )

                updateTaskStatus(downloadTask.taskInformation.taskID,TaskStatus.Activating)


                val chunkJobs = chunks.mapIndexed { chunkIndex,(start, end) ->
                    launch {

                        downloadChunk(
                            context = context,
                            downloadUrl = downloadTask.taskInformation.downloadUrl,
                            destination = Uri.parse(downloadTask.taskInformation.storagePath),
//                            taskID = downloadTask.taskInformation.taskID,
                            rangeStart = start, rangeEnd = end, chunkIndex = chunkIndex,
//                            activeState = activeState,
                            taskActiveState = currentTaskStatusFlow,
                            downloadCallback = { current,total ->

                                //taskProgress update Area
                                currentChunkProgress[chunkIndex] = current.toInt()

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
                                if(current >= total){
                                    //已完成标志
                                    currentChunkProgress[chunkIndex] = chunkFinished
                                    println("${downloadTask.taskInformation.taskName} chunkIndex:$chunkIndex completed.")

                                    if ( true == downloadingTaskFlow.value.find {
                                            it.taskInformation.taskID == downloadTask.taskInformation.taskID
                                        }?.chunkProgress?.all { it == chunkFinished }){
                                        updateTaskStatus(
                                            downloadTask.taskInformation.taskID,
                                            TaskStatus.Finished
                                        )

                                        removeTask(context = context,taskID = downloadTask.taskInformation.taskID)
                                    }


                                }


                            }
                        )



                    }
                }

                chunkJobs.joinAll()


            }

            catch (e: Exception) {
                println("Download failed: ${URL(downloadTask.taskInformation.downloadUrl)}, error: ${e.toString()}")
                updateTaskStatus(downloadTask.taskInformation.taskID,TaskStatus.Stopped)

            }

        }
        jobs.add(job)


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
    ) = downloadingTaskState.value.map{ task ->

        if(task.taskInformation.taskID == taskID){
            propTransform(task)
        }

        else{
            task
        }

    }


    private fun updateTaskProgress(
        taskID: String,
        chunkProgressList: MutableList<Int>,
        currentSpeed: Long,
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

    fun updateTaskStatus(
        taskID: String,
        taskStatus: TaskStatus,
    ){

        updateTaskInformation(taskID) {
            it.copy(taskStatus = taskStatus)

        }

        Log.d("taskStatus", "New task status: ${downloadingTaskFlow.value.find { it.taskInformation.taskID == taskID }?.taskStatus}")


    }

    fun removeTask(
        context: Context,
        taskID: String?,
        isFinished: Boolean = false,
    ){

        var selectedTask: DownloadTask? = null

        if(isFinished){
            selectedTask = downloadingTaskFlow.value.find { task -> task.taskInformation.taskID == taskID }
        }

        else{
            selectedTask = finishedTaskFlow.value.find { task -> task.taskInformation.taskID == taskID }
        }

        Log.d("taskInfo","try remove ${selectedTask?.taskInformation?.taskID}")


        selectedTask?.let {

            if(isFinished){
                finishedTaskState.value =
                    finishedTaskState.value - selectedTask

                scope.launch{
                    MyDataStore.removeTask(context = context,taskID = taskID)
                }


            }

            else{

                if(selectedTask.taskStatus != TaskStatus.Activating){
                    downloadingTaskState.value =
                        downloadingTaskState.value - selectedTask
                    Log.d("taskInfo","download UI delete")

                    if(selectedTask.taskStatus == TaskStatus.Finished){
                        finishedTaskState.value = finishedTaskState.value + selectedTask
                    }

                    else{
                        scope.launch{
                            MyDataStore.removeTask(context = context,taskID = taskID)
                            Log.d("taskInfo","download storage delete")
                        }
                    }

                }

                else{
                    updateTaskStatus(selectedTask.taskInformation.taskID, TaskStatus.Paused)
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
    private fun getFileSize(downloadUrl: String, chunkRequestFallback: () -> Unit = {}): Long? {
        try{

            var contentLength = 0L

            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"


            contentLength = connection.contentLengthLong

            if(contentLength == -1L){
                chunkRequestFallback()

            }


            return contentLength
        }

        catch (e: Exception) {
            println("get HEAD Failed, error: ${e.toString()}")
        }

        return null

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
//        activeState: MutableStateFlow<Boolean> = MutableStateFlow(true),
        taskActiveState: StateFlow<TaskStatus?>? = null,
        downloadCallback: ProgressCallback = { current,total -> },

    ): Long {

        var contentLength = 0L

        withContext(Dispatchers.IO) {
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("Range", "bytes=$rangeStart-$rangeEnd")

            val buffer = ByteArray(8 * 1024)
            val totalLength = rangeEnd - rangeStart
            var totalBytesRead = 0L

            if(rangeStart == 0L && rangeEnd == 1L){
                contentLength = connection.contentLengthLong
            }



            else{
                //TODO 不能直接使用 connection 趁早替换为 retroFit2
                connection.inputStream.use { input ->
                    context.contentResolver.openFileDescriptor(destination, "rw")?.use { pfd ->
                        FileOutputStream(pfd.fileDescriptor).use { fos ->
                            fos.channel.use { channel ->
                                channel.position(rangeStart) // 初始定位到分块起始位置

                                var currentBytesRead: Int
                                while (input.read(buffer).also { currentBytesRead = it } != -1) {
                                    fos.write(buffer, 0, currentBytesRead) // 自动更新文件指针
                                    totalBytesRead += currentBytesRead

                                    if(TaskStatus.Paused == taskActiveState?.value){
                                        Log.d("taskStatus","$chunkIndex triggered hang up")


                                        taskActiveState.first { TaskStatus.Activating == it  } //hangup : State wait for the update

                                        Log.d("taskStatus","$chunkIndex triggered resume")
                                    }

                                    // 模拟延迟和回调
                                    delay((Random.nextFloat()*1500).toLong())
                                    Log.d("taskStatus","$chunkIndex ${taskActiveState?.value}") //Pending

                                    downloadCallback(totalBytesRead, totalLength)
                                }
                            }
                        }
                    }
                }
                println("Finished chunk: $rangeStart-$rangeEnd")
            }




        }

        return contentLength
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
