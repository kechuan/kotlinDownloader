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
import androidx.compose.foundation.layout.width

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.kotlinstart.internal.TaskStatus
import kotlin.collections.find

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTile(
    taskID: String,
    taskName: String,
    progress: Float,
    currentSpeed: Long,
    totalSize: Long,
    onClick: ()-> Unit,
    onLongClick: ()-> Unit
){

    val currentStatus = MultiThreadDownloadManager.downloadingTaskFlow.value.find {
        it.taskInformation.taskID == taskID &&
                it.taskStatus == TaskStatus.Activating || it.taskStatus == TaskStatus.Paused
    }?.taskStatus

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
                        enabled = true,
                        onClick = {
                            if( TaskStatus.Paused == currentStatus ){
                                MultiThreadDownloadManager.updateTaskStatus(taskID, taskStatus = TaskStatus.Activating)
                            }

                            else{
                                MultiThreadDownloadManager.updateTaskStatus(taskID, taskStatus = TaskStatus.Paused)
                            }

                        }
                    )
            ){
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Pause"
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
                                    "${convertBinaryType((progress*totalSize).toLong())}/${convertBinaryType(totalSize)}"
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

