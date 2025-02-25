package com.example.kotlinstart.internal

import TaskStatus
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class TaskInformation(
    val taskID: String = "taskID",
    val taskName: String = "taskName",
    val downloadUrl: String = "downloadUrl",
    val storagePath: String = "storagePath",
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
    val fileSize: Long = 0,
    val currentSpeed: Long = 0,
    val threadCount: Int = 1,
    val speedLimit: Long = 0,
//    val failureReason: String? = null
){
    companion object{
        val Default = DownloadTask()
    }
}