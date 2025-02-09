import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URL
import kotlin.collections.mutableListOf


data class TaskData(
    val taskName: String = "newTask",
    val progress: Float = 0.0F,
    val speed: Long = 0,
    val storagePath: String = "",
){
    companion object{
        val Default = TaskData()
    }
}

object DownloadViewModel: ViewModel() {

    val downloadingTasksState = MutableStateFlow<List<TaskData>>(emptyList())
    val finishedTaskState = MutableStateFlow<List<TaskData>>(emptyList())

    val downloadingTasksFlow: StateFlow<List<TaskData>> = downloadingTasksState.asStateFlow()
    val finishedTasksFlow: StateFlow<List<TaskData>> = finishedTaskState.asStateFlow()

    val localDownloadNavController = staticCompositionLocalOf<NavController> {
        error("ImageNavController not provided")
    }

    fun addTask(
        taskName: String,
        storagePath: String
    ){

        viewModelScope.launch{
            downloadingTasksState.value =
                downloadingTasksFlow.value +
                TaskData(taskName = taskName, storagePath = storagePath)

//            downloadingTasksState.value +=
//                TaskData(taskName = taskName, storagePath = storagePath)
        }

    }

    fun removeTask(taskName: String){
        viewModelScope.launch {
            downloadingTasksState.value.any {
                if (it.taskName == taskName) {
                    downloadingTasksState.value - it
                    true
                }

                false
            }
        }


    }

    // 更新任务进度
    fun updateTaskProgress(taskName: String, progress: Float) {
        viewModelScope.launch {
            downloadingTasksState.value = downloadingTasksState.value.map { task ->
                if (task.taskName == taskName) {
                    //相当于 dart 里的
                    // task = task..["progress"] = newProgress 是吗?
                    task.copy(progress = progress)
                }

                else {
                    task
                }
            }
        }
    }

    fun updateAllTask(progress:List<Float>){
        viewModelScope.launch {

            var resultData : ArrayList<TaskData> = ArrayList();

            for(currentIndex in 0..(progress.size-1)){
                resultData.add(downloadingTasksState.value[currentIndex].copy(progress = progress[currentIndex]))
            }

            downloadingTasksState.value = resultData

        }

    }
}