


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.net.URL
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.util.Timer
import java.util.TimerTask
import kotlin.random.Random

typealias ProgressCallback = (current: Long, total: Long) -> Unit

data class TestDownloadTask(
    val taskID: String,
    val fileName: String,
    val downloadUrl: String,
    val storagePath: String,
    val chunkProgress: MutableList<Float> = mutableListOf(),
    val threadCount: Int = 1,
    val speedLimit: Long = 0,
)

object TestMultiThreadDownloadManager {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val jobs = mutableListOf<Job>()

    private val downloadingTaskState = MutableStateFlow<List<TestDownloadTask>>(emptyList())
    val downloadingTaskFlow: StateFlow<List<TestDownloadTask>> = downloadingTaskState.asStateFlow()

    // 添加下载任务
    fun addTask(
        TestDownloadTask: TestDownloadTask,
        chunkSize: Long = 5 * 1024 * 1024, //5MB
    ) {
        val job = scope.launch {
            try {
                println("Starting multi-thread download: ${URL(TestDownloadTask.downloadUrl)}")

                val fileSize = getFileSize(TestDownloadTask.downloadUrl)
                val chunks = calculateChunks(fileSize, chunkSize)

                val currentChunkProgress = MutableList<Float>(chunks.size){0F}


                //任务显示: 初始化
                downloadingTaskState.value = downloadingTaskFlow.value + TestDownloadTask


                // 创建文件并预分配空间
                RandomAccessFile(TestDownloadTask.storagePath, "rw").use {
                    file -> file.setLength(fileSize) // 预分配文件空间
                }

                val updateTimer = Timer()
                val updateTask = object : TimerTask(){
                    override fun run(){

                        updateTaskProgress(
                            TestDownloadTask.fileName,
                            currentChunkProgress
                        )

                        downloadingTaskFlow.value.map {
                            if(it.fileName == TestDownloadTask.fileName){
                                println("currentTask: ${it.fileName} : ${it.chunkProgress}")

                                if(it.chunkProgress.all { it == -1.0F }){
                                    updateTimer.cancel()
                                }
                            }

                        }


                    }
                }

                updateTimer.schedule(updateTask, 1000, 500)

                // 分配每个块
                val chunkJobs = chunks.map { (start, end) ->
                    launch {
                        downloadChunk(

                            url = TestDownloadTask.downloadUrl,
                            destination = TestDownloadTask.storagePath,
                            start = start,
                            end = end,
                            downloadCallback = { current,total ->
                                //List<Pair,Pair>
                                val chunkIndex = chunks.indexOfFirst { it.first == start }
                                currentChunkProgress[chunkIndex] = (current.toFloat()/total)

                                if(current == total){
                                    //已完成标志
                                    currentChunkProgress[chunkIndex] = -1.0F
                                    println("${TestDownloadTask.fileName} chunkIndex:$chunkIndex completed.")
                                }


                            }
                        )
                    }
                }

                // 并发等待所有块下载完成
                chunkJobs.joinAll()

                println("Download completed: ${URL(TestDownloadTask.downloadUrl)}")
            }

            catch (e: Exception) {
                println("Download failed: ${URL(TestDownloadTask.downloadUrl)}, error: ${e.toString()}")
            }
        }
        jobs.add(job)
    }

    private fun updateTaskProgress(
        fileName: String,
        chunkProgressList: MutableList<Float>,
        ){

            downloadingTaskState.value = downloadingTaskState.value.map { task ->
                if (task.fileName == fileName) {
                    task.copy(
                        chunkProgress = chunkProgressList
                    )
                }

                else {
                    task
                }
            }

        }


    private fun updateTaskSpeedLimit(
        fileName: String,
        speedLimit: Long,
    ){

        downloadingTaskState.value = downloadingTaskState.value.map { task ->
            if (task.fileName == fileName) {
                task.copy(speedLimit = speedLimit)
            }

            else {
                task
            }
        }

    }


    // 获取文件大小
    private fun getFileSize(url: String): Long {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        return connection.contentLengthLong
    }

    // 计算文件的块范围
    private fun calculateChunks(fileSize: Long, chunkSize: Long): List<Pair<Long, Long>> {
        val chunks = mutableListOf<Pair<Long, Long>>()
        var start = 0L
        while (start < fileSize) {
            val end = (start + chunkSize - 1).coerceAtMost(fileSize - 1)
            chunks.add(start to end)
            start += chunkSize
        }
        return chunks
    }

    // 下载单个块
    private suspend fun downloadChunk(
        url: String, destination: String = "",
        start: Long = 0L,
        end: Long = 1L,
        downloadCallback: ProgressCallback = { current,total -> }
    ): Long {

        var contentLength = 0L

        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("Range", "bytes=$start-$end")

            if(start == 0L && end == 1L){
                contentLength = connection.contentLengthLong
            }

            else{
                connection.inputStream.use { input ->
                    RandomAccessFile(destination, "rw").use { rafFile ->
                        rafFile.seek(start) // 定位到块的起始位置
                        val buffer = ByteArray(8*1024)
                        val totalLength: Long = end-start

                        var totalBytesRead: Long = 0
                        var currentBytesRead: Int = 0


                        while (input.read(buffer).also { currentBytesRead = it } != -1) {
                            rafFile.write(buffer, 0, currentBytesRead) // 将数据写入文件
                            totalBytesRead+=currentBytesRead

                            //实验: 0~1500 ms 网速
                            delay((Random.nextDouble()*1500).toLong()).run { println(" ${Thread.currentThread().name} [$start/$end] $totalBytesRead/${totalLength} ") }

                            downloadCallback(totalBytesRead,totalLength)

                            //但不符合个人需求 我需要。。多个分块叠加在一起的进度 划为速度。。

                        }
                    }
                }
                println("Downloaded chunk: $start-$end")
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

//enum class BinaryType(
//    val binaryType: String,
//    val size: Long
//){
//    B("B",1024),
//    KB("KB",1024*1024),
//    MB("MB",1024*1024*1024),
//    GB("GB",1024*1024*1024*1024)
//}

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
    var resultType = com.example.kotlinDownloader.internal.BinaryType.B.binaryType
    var resultValue = value.toFloat()

    com.example.kotlinDownloader.internal.BinaryType.entries.any {
        //1000*1024 => 1000 KB => 0.97MB

        if(resultValue<1024) {
            //划分之后 如果数值超过 1000 那么应该再切割一下 并更新Type
            resultType = it.binaryType

            if(resultValue>=1000){
                resultValue /= com.example.kotlinDownloader.internal.BinaryType.KB.size
                return@any false
            }

            else{
                return@any true
            }

        }

        else{
            resultValue /= com.example.kotlinDownloader.internal.BinaryType.KB.size
        }

        return@any false



    }


    var splitResult = resultValue.toString().split(".")

    if(splitResult.last().length <= 2){
        return "$resultValue$resultType"
    }

    return "${splitResult[0]}.${splitResult[1].substring(0,2)}$resultType"


}
