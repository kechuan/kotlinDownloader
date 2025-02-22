import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.twotone.StopCircle

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.kotlinstart.internal.MultiThreadDownloadManager
import com.example.kotlinstart.internal.MultiThreadDownloadManager.downloadingTaskFlow
import com.example.kotlinstart.internal.TaskStatus
import com.example.kotlinstart.internal.TaskStatus.*
import com.example.kotlinstart.internal.convertBinaryType
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
    taskName: String,
    progress: Float,
    currentSpeed: Long,
    totalSize: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit
){

    val localContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentTaskStatusFlow: StateFlow<TaskStatus?> = downloadingTaskFlow
        .map{
            it.firstOrNull{ it.taskInformation.taskID == taskID }?.taskStatus
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly, //当下载开始时 (downloadChunk回调) 开始监听
            initialValue = downloadingTaskFlow.value.find { it.taskInformation.taskID == taskID }?.taskStatus
        )

    val taskStatusState by currentTaskStatusFlow.collectAsState()

    var iconStatus = Icons.Default.History

    when(taskStatusState){
        Pending -> iconStatus = Icons.Default.History
        Activating -> iconStatus = Icons.Default.Pause
        Paused -> iconStatus = Icons.Default.PlayArrow
        Finished -> iconStatus = Icons.Default.Done
        Stopped -> iconStatus = Icons.Default.StopCircle
        null -> {}
    }

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
                            if( Paused == taskStatusState ){
                                coroutineScope.launch {
                                    MultiThreadDownloadManager.updateTaskStatus(
                                        context = localContext,
                                        taskID = taskID,
                                        taskStatus = Activating
                                    )

                                    MultiThreadDownloadManager.addTask(
                                        context = localContext,
                                        downloadTask = downloadingTaskFlow.value.find{ it.taskInformation.taskID == taskID },
                                        isResume = true,
                                    )


                                }

                            }

                            else{
                                if( Finished != taskStatusState) {
                                    coroutineScope.launch {
                                        MultiThreadDownloadManager.updateTaskStatus(
                                            context = localContext,
                                            taskID = taskID,
                                            taskStatus = Paused
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

                Text(text = taskName)

                Box {

                    LinearProgressIndicator(
                        modifier = Modifier
                            .height(height = 20.dp)
                            .fillMaxWidth()
                        ,
                        progress = { progress },

                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween
                    ){
//                        println("Before: taskName:$taskName, $progress/$lastProgress")

                        Text(
                            text = "speed: ${convertBinaryType(currentSpeed)}/s",
//                            text = "speed: $currentSpeed",
                            style = TextStyle(color = Color.Black)
                        )

                        Text(text =
                            "size: ${
                                if(totalSize == 0L) {
                                    "正在查询信息..."
                                } 
                                
                                else {
                                    "${convertBinaryType((progress * totalSize).toLong())}/${convertBinaryType(totalSize)}"
                                }
                            }")

                    }



                }
            }
        },

        trailingContent = {
//            Checkbox(checked = multiCheckStatus, onCheckedChange = {multiCheckStatus=!multiCheckStatus} )
        },
    )
}

