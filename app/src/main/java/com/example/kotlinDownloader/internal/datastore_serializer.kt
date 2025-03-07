package com.example.kotlinDownloader.internal


import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

//private Path /data/data/[package_name]/files/datastore/taskData.preferences_pb
//val Context.taskData: DataStore<DownloadTask> by preferencesDataStore(name = "taskData")

//基础类型存储 (键值对)
//val Context.settings: DataStore<Preferences> by preferencesDataStore(name = "settings")
//val settingStoragePath = stringPreferencesKey("storagePath")


// 复杂类型存储 (TypeAdapter)
val Context.tasksData: DataStore<DownloadTasks> by dataStore(
    fileName = "tasks_data.pb",
    serializer = DownloadTasksSerializer
)

val Context.settingsConfig: DataStore<AppConfigs> by dataStore(
    fileName = "settings_config.pb",
    serializer = AppConfigsSerializer
)


@Serializable
data class DownloadTasks(
    val tasks: List<DownloadTask> = emptyList()
)

@Serializable
data class AppConfigs(
    val storagePath: String = "storagePath",
    val threadLimit: Int = 16,
    val speedLimit: Long = 10*BinaryType.MB.size
){
    companion object{
        val Default = AppConfigs()
    }
}

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

object AppConfigsSerializer : Serializer<AppConfigs> {
    override val defaultValue: AppConfigs = AppConfigs()

    override suspend fun readFrom(input: InputStream): AppConfigs {
        try {
            return Json.decodeFromString(
                AppConfigs.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read download tasks data: ${e.message}", e)
        }
    }

    override suspend fun writeTo(task: AppConfigs, output: OutputStream) {
        output.write(
            Json.encodeToString(AppConfigs.serializer(), task)
                .encodeToByteArray()
        )
    }
}

object AppConfig{
    var appConfigs = AppConfigs.Default

    suspend fun init(context: Context){
        appConfigs = getAppConfig(context)
    }

    suspend fun updateAppConfig(
        context: Context,
        newAppConfigs:AppConfigs?,
    ){
        newAppConfigs?.let{
            context.settingsConfig.updateData { current -> newAppConfigs }
            appConfigs = newAppConfigs
        }
    }

    suspend fun getAppConfig(context: Context):AppConfigs = context.settingsConfig.data.first()


}


object MyDataStore{

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
        context.tasksData.updateData { current -> DownloadTasks() }
    }


    fun getAllTasks(context: Context) = context.tasksData.data.map { it.tasks }


}


