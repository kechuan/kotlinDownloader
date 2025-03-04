package com.example.kotlinDownloader.internal
import androidx.datastore.core.CorruptionException

import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.map


import java.io.InputStream
import java.io.OutputStream

//private Path /data/data/[package_name]/files/datastore/taskData.preferences_pb
//val Context.taskData: DataStore<DownloadTask> by preferencesDataStore(name = "taskData")

//基础类型存储 (键值对)
val Context.settings: DataStore<Preferences> by preferencesDataStore(name = "settings")
val settingStoragePath = stringPreferencesKey("storagePath")


// 复杂类型存储 (TypeAdapter)
val Context.tasksData: DataStore<DownloadTasks> by dataStore(
    fileName = "tasks_data.pb",
    serializer = DownloadTasksSerializer
)



@Serializable
data class DownloadTasks(
    val tasks: List<DownloadTask> = emptyList()
)

object DownloadTasksSerializer : Serializer<DownloadTasks> {
    override val defaultValue: DownloadTasks = DownloadTasks()

    override suspend fun readFrom(input: InputStream): DownloadTasks {
        try {
            return Json.decodeFromString(
                DownloadTasks.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read download tasks data: ${e.message}", e)
        }
    }

    override suspend fun writeTo(task: DownloadTasks, output: OutputStream) {
        output.write(
            Json.encodeToString(DownloadTasks.serializer(), task)
                .encodeToByteArray()
        )
    }
}

object MyDataStore{

    var storagePath: Uri? = null

    suspend fun init(context: Context){
        context.settings.data.map{ preferences ->
            preferences[settingStoragePath]
        }.first()?.let{
            storagePath = Uri.parse(it)
        }
    }

    suspend fun addTask(context: Context, newTask:DownloadTask){
        context.tasksData.updateData { current ->
            DownloadTasks(current.tasks + newTask)
        }
    }

    suspend fun updateTask(
        context: Context,
        taskID: String?,
        newTaskInformation:DownloadTask?,
    ){

        newTaskInformation?.let{
            context.tasksData.updateData { current ->

                DownloadTasks(
                    current.tasks.map {
                        if(it.taskInformation.taskID == taskID){
                            newTaskInformation
                        }

                        else{
                            it
                        }
                    }
                )
        }




        }
    }

    suspend fun removeTask(context: Context, taskID:String?){
        taskID?.let{ taskID ->
            context.tasksData.updateData { current ->
                DownloadTasks(current.tasks.filter { it.taskInformation.taskID != taskID }) //透过 filter 实现的 删除
            }
        }
    }

    suspend fun removeTasks(
        context: Context,
        taskIDList: List<String>
    ){
        context.tasksData.updateData { current ->
            DownloadTasks(current.tasks.filter { storageTask ->
                !taskIDList.any { it == storageTask.taskInformation.taskID }
            }) //任一符合 taskIDList 的数据则被筛去
        }
    }

    suspend fun removeAllTasks(context: Context){
        context.tasksData.updateData { current ->  DownloadTasks() }
    }


    fun getAllTasks(context: Context) = context.tasksData.data.map { it.tasks }


}


