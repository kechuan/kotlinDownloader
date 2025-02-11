


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException

import java.net.URL
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random

typealias ProgressCallback = (current: Long, total: Long) -> Unit

enum class TaskStatus{
    Pending,
    Activating,
    Finished,
    Paused,
    Stopped
}

const val chunkFinished = -1

data class DownloadTask(
    val taskID: String,
    val taskName: String,
    val downloadUrl: String,
    val storagePath: Uri,
    var taskStatus: TaskStatus = TaskStatus.Pending,
    var fileSize: Long = 0,
    var chunkProgress: MutableList<Int> = mutableListOf(),
    var threadCount: Int = 1,
    var speedLimit: Long = 0,
)

object MultiThreadDownloadManager {


    private val scope = CoroutineScope(Dispatchers.IO)
    private val jobs = mutableListOf<Job>()

    private val downloadingTaskState = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadingTaskFlow: StateFlow<List<DownloadTask>> = downloadingTaskState.asStateFlow()

    // 添加下载任务
    fun addTask(
        context: Context,
        downloadTask: DownloadTask,
        chunkCount: Int = 5,
//        chunkSize: Long = 2 * 1024 * 1024, //5MB
    ) {

        val job = scope.launch {
            try {

                var fileSize = 0L

                //没有三目表达式的情况之下 真的很不喜欢把 if/else 写在一个变量定义里
                //假如其已经在dialog里获取到信息就直接沿用它的信息
                if(downloadTask.fileSize != 0L){
                    fileSize = downloadTask.fileSize
                }

                else{
                    fileSize = getFileSize(downloadTask.downloadUrl) ?: return@launch
                    //如果获取不到信息 暂且先直接退出
                }

                val chunks = calculateChunks(fileSize, chunkCount)

                //Int模式 利于保存
                val currentChunkProgress = MutableList<Int>(chunks.size){0}


                downloadTask.also {
                    it.fileSize = fileSize
                    it.chunkProgress = currentChunkProgress
                }

                //UI任务显示: 初始化
                downloadingTaskState.value = downloadingTaskFlow.value + downloadTask

                println("Starting multi-thread download: ${URL(downloadTask.downloadUrl)} chunks:$chunks currentTask: $downloadTask")

                preallocateSpace(
                    context = context,
                    uri = downloadTask.storagePath,
                    targetSize = fileSize
                )

                //task progress update
                //每个任务独立一个计时器来更新UI
                val updateTimer = Timer()
                val updateTask = object : TimerTask(){
                    override fun run(){

                        if(downloadTask.taskStatus == TaskStatus.Paused){
                            //保存.. range信息? 算了 初期直接stop好了
                            //range信息保存? 干脆放到 DataStorage 好了?

                            Log.d("taskInfo","task is paused")
                            this@launch.cancel()
                        }

                        updateTaskProgress(
                            taskID = downloadTask.taskName,
                            chunkProgressList = currentChunkProgress
                        )

                        downloadingTaskFlow.value.map {
                            if(it.taskName == downloadTask.taskName){
                                println("currentTask: ${it.taskName} : ${it.chunkProgress}")

                                if(it.chunkProgress.all { it == chunkFinished }){
                                    updateTaskStatus(
                                        downloadTask.taskName,
                                        TaskStatus.Finished
                                    )
                                    updateTimer.cancel()
                                }
                            }

                        }


                    }
                }

                updateTimer.schedule(updateTask, 1000, 500)

                var chunkIndex = 0

                // 分配每个块
                val chunkJobs = chunks.map { (start, end) ->
                    launch {

                        downloadChunk(
                            context = context,
                            downloadUrl = downloadTask.downloadUrl,
                            destination = downloadTask.storagePath,
                            rangeStart = start, rangeEnd = end, chunkIndex = chunkIndex,
                            downloadCallback = { current,total ->
                                //List<Pair,Pair>

                                currentChunkProgress[chunkIndex] = current.toInt()

                                //有时候就是会出现 3068581/3068580 => 1.0000004 这样的情况 我也不知道为什么
                                if(current >= total){
                                    //已完成标志
                                    currentChunkProgress[chunkIndex] = chunkFinished
                                    println("${downloadTask.taskName} chunkIndex:$chunkIndex completed.")
                                }

//                                else{
//                                    if(current.toFloat()/total > 0.95){
//                                        println("Callback: $chunkIndex $current/$total ${current.toFloat()/total} ")
//                                    }
//
//                                }


                            }
                        )

                        chunkIndex+=1

                    }
                }

                chunkJobs.joinAll()


            }

            catch (e: Exception) {
                println("Download failed: ${URL(downloadTask.downloadUrl)}, error: ${e.toString()}")
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
    ){
        downloadingTaskState.value = downloadingTaskState.value.map { task ->
            if (task.taskID == taskID) {
                propTransform(task)
            }

            else {
                task
            }
        }
    }

    private fun updateTaskProgress(
        taskID: String,
        chunkProgressList: MutableList<Int>,
    ) = updateTaskInformation(taskID) { it.copy(chunkProgress = chunkProgressList) }

    private fun updateTaskSpeedLimit(
        taskID: String,
        speedLimit: Long,
    ) = updateTaskInformation(taskID) { it.copy(speedLimit = speedLimit) }

    private fun updateTaskStatus(
        taskID: String,
        taskStatus: TaskStatus,
    ) = updateTaskInformation(taskID) { it.copy(taskStatus = taskStatus) }

    private fun removeTask(taskID: String?){


        var selectedTask: DownloadTask? = null
        selectedTask = downloadingTaskFlow.value.find { task -> task.taskID == taskID }

        selectedTask?.let {

            //需等待暂停流程 才可移除 pauseTask


            if(selectedTask.taskStatus != TaskStatus.Activating){
                downloadingTaskState.value =
                    downloadingTaskState.value - selectedTask
            }

        }
    }

    // 获取文件大小
    private fun getFileSize(url: String): Long? {
        try{
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            return connection.contentLengthLong
        }

        catch (e: Exception) {
            println("get HEAD Failed, error: ${e.toString()}")
        }

        return null

    }

    // 计算文件的块范围
//    private fun calculateChunks(fileSize: Long, chunkSize: Long): List<Pair<Long, Long>> {
//        val chunks = mutableListOf<Pair<Long, Long>>()
//        var start = 0L
//        while (start < fileSize) {
//            val end = (start + chunkSize - 1).coerceAtMost(fileSize - 1)
//            chunks.add(start to end)
//            start += chunkSize
//        }
//        return chunks
//    }

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
        destination: Uri,
        rangeStart: Long, rangeEnd: Long, chunkIndex: Int,
        downloadCallback: ProgressCallback,

    ) {

        withContext(Dispatchers.IO) {
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("Range", "bytes=$rangeStart-$rangeEnd")

            val buffer = ByteArray(8 * 1024)
            val totalLength = rangeEnd - rangeStart
            var totalBytesRead = 0L



            connection.inputStream.use { input ->
                context.contentResolver.openFileDescriptor(destination, "rw")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { fos ->
                        fos.channel.use { channel ->
                            channel.position(rangeStart) // 初始定位到分块起始位置

                            var currentBytesRead: Int
                            while (input.read(buffer).also { currentBytesRead = it } != -1) {
                                fos.write(buffer, 0, currentBytesRead) // 自动更新文件指针
                                totalBytesRead += currentBytesRead

                                // 模拟延迟和回调
                                println("${Thread.currentThread().name} chunkIndex:$chunkIndex $totalBytesRead/$totalLength")
                                downloadCallback(totalBytesRead, totalLength)
                            }
                        }
                    }
                }
            }
            println("Finished chunk: $rangeStart-$rangeEnd")
        }

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
