import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager.downloadingTaskFlow
import com.example.kotlinDownloader.internal.TaskStatus
import com.example.kotlinDownloader.internal.convertBinaryType
import com.example.kotlinDownloader.internal.convertIconState
import com.example.kotlinDownloader.models.DownloadViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.find

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTile(
    taskID: String,
    fileName: String,
    progress: Float,
    currentSpeed: Long,
    totalSize: Long,
    multiChooseMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
){

    val localContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var taskMessage: String? = null

    val currentTaskStatusFlow: StateFlow<TaskStatus?> = downloadingTaskFlow
        .map{
            //每次更新 status 的时候 顺带获取更新 message 内容
            taskMessage = it.firstOrNull{ it.taskInformation.taskID == taskID }?.message
            it.firstOrNull{ it.taskInformation.taskID == taskID }?.taskStatus
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly, //当下载开始时 (downloadChunk回调) 开始监听
            initialValue = downloadingTaskFlow.value.find { it.taskInformation.taskID == taskID }?.taskStatus
        )

    val taskStatusState by currentTaskStatusFlow.collectAsState()

    var iconStatus = convertIconState(taskStatusState)


    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        leadingContent = {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable(
                        onClick = {
                            if (taskStatusState == TaskStatus.Paused ||
                                taskStatusState == TaskStatus.Stopped
                            ) {
                                coroutineScope.launch {
                                    MultiThreadDownloadManager.updateTaskStatus(
                                        context = localContext,
                                        taskID = taskID,
                                        taskStatus =
                                            if (taskStatusState == TaskStatus.Stopped) TaskStatus.Pending
                                            else TaskStatus.Activating
                                    )

                                    MultiThreadDownloadManager.addTask(
                                        context = localContext,
                                        downloadTask = downloadingTaskFlow.value.find { it.taskInformation.taskID == taskID },
                                        isResume = true,
                                    )


                                }

                            } else {
                                if (TaskStatus.Finished != taskStatusState) {
                                    coroutineScope.launch {
                                        MultiThreadDownloadManager.updateTaskStatus(
                                            context = localContext,
                                            taskID = taskID,
                                            taskStatus = TaskStatus.Paused
                                        )
                                    }

                                }


                            }

                        }
                    )
            ){
                Icon(
                    imageVector = iconStatus,
                    contentDescription = "status toggle"
                )
            }
        },
        headlineContent = {
            Column {

                Text(text = fileName)

                Box {

                    taskMessage?.let{
                        Text(it)
                    }

                    LinearProgressIndicator(
                        modifier = Modifier
                            .height(height = 20.dp)
                            .fillMaxWidth()
                        ,
                        progress = {
                            if(taskMessage != null) 0F
                            else progress
                        },

                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ){

                        Text(
                            text = "${convertBinaryType(currentSpeed)}/s",
                            style = TextStyle(color = Color.Black)
                        )

                        Box(
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ){
                            Text("|")
                        }

                        Text(
                            text =
                                "size: ${
                                    if(totalSize == 0L) {
                                        "正在查询信息..."
                                    }
                                    else {
                                        "${convertBinaryType((progress * totalSize).toLong())}/${convertBinaryType(totalSize)}"
                                    }
                                }",
                        )

                    }


                }
            }
        },
        trailingContent = {


            if(multiChooseMode){

                var containStatus by remember { mutableStateOf(false) }

                Checkbox(
                    checked = containStatus,
                    onCheckedChange = {
                        if(containStatus){
                            DownloadViewModel.MultiChooseSet.remove(taskID)
                        }
                        else {
                            DownloadViewModel.MultiChooseSet.add(taskID)
                        }

                        containStatus = !containStatus
                    }
                )
            }

        },
    )
}

