package com.example.kotlinstart.internal

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
enum class TaskStatus{
    Pending,
    Activating,
    Finished,
    Paused,
    Stopped
}

@Serializable
data class DownloadTaskMap(
    val tasks: Map<String, DownloadTask> = emptyMap()
)

@Serializable
@Immutable
data class TaskInformation(
    val taskID: String = "taskID",
    val taskName: String = "taskName",
    val downloadUrl: String = "downloadUrl",
    val storagePath: String = "storagePath",
){
    companion object{
        val Default = TaskInformation()
    }
}

@Serializable
data class DownloadTask(
    val taskInformation: TaskInformation = TaskInformation.Default,
    val taskStatus: TaskStatus = TaskStatus.Pending,
    val chunkProgress: List<Int> = emptyList(), //重建以更新
    val fileSize: Long = 0,
    val currentSpeed: Long = 0,
    var threadCount: Int = 1,
    var speedLimit: Long = 0,
){
    companion object{
        val Default = DownloadTask()
    }
}