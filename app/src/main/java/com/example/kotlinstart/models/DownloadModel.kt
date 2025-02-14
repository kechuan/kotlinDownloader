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



object DownloadViewModel: ViewModel() {

//    val MultiThreadDownloadManager.downloadingTasksState = MutableStateFlow<List<TaskData>>(emptyList())

    val localDownloadNavController = staticCompositionLocalOf<NavController> {
        error("ImageNavController not provided")
    }

    val downloadingTasksFlow: StateFlow<List<DownloadTask>> = MultiThreadDownloadManager.downloadingTaskFlow
    val finishedTasksFlow: StateFlow<List<DownloadTask>> = MultiThreadDownloadManager.finishedTaskFlow





}