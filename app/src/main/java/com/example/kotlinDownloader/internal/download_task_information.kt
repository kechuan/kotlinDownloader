package com.example.kotlinDownloader.internal

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class TaskInformation(
    val taskID: String = "taskID",
    val fileName: String = "fileName",
    val downloadUrl: String = "downloadUrl",
    val storagePath: String = "storagePath",
    val fileSize: Long = 0L,
    val chunksRangeList: List<Pair<Long, Long>> = emptyList(),
){
    companion object{
        val Default = TaskInformation()
    }
}

@Serializable
@Immutable
data class DownloadTask(
    val taskInformation: TaskInformation = TaskInformation.Default,
    val taskStatus: TaskStatus = TaskStatus.Pending,
    val chunkProgress: List<Int> = emptyList(), //重建以更新
    val currentSpeed: Long = 0,
    val speedLimit: Long = 0,
    val message: String? = null
){
    companion object{
        val Default = DownloadTask()
    }
}