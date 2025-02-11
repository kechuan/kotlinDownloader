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

//    val MultiThreadDownloadManager.downloadingTasksState = MutableStateFlow<List<TaskData>>(emptyList())

    val localDownloadNavController = staticCompositionLocalOf<NavController> {
        error("ImageNavController not provided")
    }

    val downloadingTasksFlow: StateFlow<List<DownloadTask>> = MultiThreadDownloadManager.downloadingTaskFlow

    val finishedTaskState = MutableStateFlow<List<TaskData>>(emptyList())
    val finishedTasksFlow: StateFlow<List<TaskData>> = finishedTaskState.asStateFlow()


//    fun removeTask(taskName: String){
//        viewModelScope.launch {
//            MultiThreadDownloadManager.downloadingTasksState.value.any {
//                if (it.taskName == taskName) {
//                    MultiThreadDownloadManager.downloadingTasksState.value - it
//                    true
//                }
//
//                false
//            }
//        }
//
//
//    }

//    // 更新任务进度
//    fun updateTaskProgress(taskName: String, progress: Float) {
//        viewModelScope.launch {
//            MultiThreadDownloadManager.downloadingTasksState.value = MultiThreadDownloadManager.downloadingTasksState.value.map { task ->
//                if (task.taskName == taskName) {
//                    //相当于 dart 里的
//                    // task = task..["progress"] = newProgress 是吗?
//                    task.copy(progress = progress)
//                }
//
//                else {
//                    task
//                }
//            }
//        }
//    }

//    fun updateAllTask(progress:List<Float>){
//        viewModelScope.launch {
//
//            var resultData : ArrayList<TaskData> = ArrayList();
//
//            for(currentIndex in 0..(progress.size-1)){
//                resultData.add(MultiThreadDownloadManager.downloadingTasksState.value[currentIndex].copy(progress = progress[currentIndex]))
//            }
//
//            MultiThreadDownloadManager.downloadingTasksState.value = resultData
//
//        }
//
//    }
}