import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.kotlinstart.internal.DownloadTask
import com.example.kotlinstart.internal.MultiThreadDownloadManager
import kotlinx.coroutines.flow.StateFlow


object DownloadViewModel: ViewModel() {

//    val MultiThreadDownloadManager.downloadingTasksState = MutableStateFlow<List<TaskData>>(emptyList())

    val localDownloadNavController = staticCompositionLocalOf<NavController> {
        error("ImageNavController not provided")
    }

    val downloadingTasksFlow: StateFlow<List<DownloadTask>> = MultiThreadDownloadManager.downloadingTaskFlow
    val finishedTasksFlow: StateFlow<List<DownloadTask>> = MultiThreadDownloadManager.finishedTaskFlow





}