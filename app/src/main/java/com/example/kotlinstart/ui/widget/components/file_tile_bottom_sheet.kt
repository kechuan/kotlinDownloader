
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*



import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow


import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape

import androidx.compose.ui.text.style.TextOverflow


import com.example.kotlinstart.internal.DownloadTask
import com.example.kotlinstart.internal.MultiThreadDownloadManager
import com.example.kotlinstart.internal.MultiThreadDownloadManager.downloadingTaskFlow
import com.google.relay.compose.BoxScopeInstanceImpl.align

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTileBottomSheet(
    selectingTask: DownloadTask,
    onDismiss: () -> Unit = {},
) {

    val localContext = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val chunkProgress by downloadingTaskFlow
        .map {
            it.firstOrNull { it.taskInformation.taskID == selectingTask.taskInformation.taskID }?.chunkProgress
        }
        .distinctUntilChanged()
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = DownloadTask.Default.chunkProgress
        )
        .collectAsState()


    val currentTaskStatus by downloadingTaskFlow
        .map {
            it.firstOrNull { it.taskInformation.taskID == selectingTask.taskInformation.taskID }?.taskStatus
        }
        .distinctUntilChanged()
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = TaskStatus.Pending
        )
        .collectAsState()



    var fileName by remember { mutableStateOf(selectingTask.taskInformation.taskName) }

    var speedLimitRange by remember { mutableFloatStateOf((selectingTask.speedLimit.toFloat())/(50* BinaryType.MB.size)) } // 0 表示不限速
    var threadCount by remember { mutableIntStateOf(selectingTask.threadCount) }

    //期望: Column -> taskName/(Info)chunkProgress + basicAction[/Pause/resume/Delete]/(control)
    //TabRows(overview/Details)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = {},
//        shape = BottomSheetDefaults.
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {

                //Title : Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){

                    BoxWithConstraints {
                        Text(
                            modifier = Modifier
                                .width(maxWidth - 80.dp),
                            text = fileName,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2,
                        )
                    }

                    Row {
                        Icon(
                            imageVector = convertIconState(currentTaskStatus),
                            contentDescription = currentTaskStatus?.name,
                            modifier = Modifier.size(16.dp)

                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text = "${currentTaskStatus?.name}")
                    }

                }

                Spacer(modifier = Modifier.height(16.dp))

                DownloadProgressBar(
                    downloadTask = selectingTask,
                    chunkProgress = chunkProgress,
                )

                //Action Area
                Column {

                    val targetStatus = if(currentTaskStatus == TaskStatus.Activating){
                        TaskStatus.Paused
                    }

                    else{
                        TaskStatus.Activating
                    }


                    ListItem(
                        modifier = Modifier
                            .clickable(
                                onClick = {

                                    if(currentTaskStatus == TaskStatus.Finished) return@clickable

                                    coroutineScope.launch {
                                        MultiThreadDownloadManager.updateTaskStatus(
                                            context = localContext,
                                            taskID = selectingTask.taskInformation.taskID,
                                            taskStatus = targetStatus
                                        )
                                    }

                                }
                            )

                        ,

                        headlineContent = {

                            if(currentTaskStatus == TaskStatus.Activating){
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause")
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(text = "暂停")

                                }
                            }

                            else{

                                if(currentTaskStatus != TaskStatus.Finished) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ){
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume")
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(text = "恢复")

                                    }
                                }

                            }


                        }
                    )

                    HorizontalDivider()

                    ListItem(
                        modifier = Modifier
                            .clickable(
                                onClick = {

                                    coroutineScope.launch {
                                        MultiThreadDownloadManager.removeTask(
                                            context = localContext,
                                            taskID = selectingTask.taskInformation.taskID,
                                        )
                                    }


                                }
                            )

                        ,
                        headlineContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Delete")
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "删除")
                            }
                        }
                    )

                    HorizontalDivider()

                    ListItem(
                        modifier = Modifier
                            .clickable(
                                onClick = {


                                    //DialogShow
    //                                TextField(
    //                                    value = fileName,
    //                                    onValueChange = {
    //                                        fileName = it
    //
    //                                    },
    //                                    label = { Text("保存名称") },
    //                                    modifier = Modifier.fillMaxWidth()
    //                                )


                                }
                            )

                        ,
                        headlineContent = {
                            Row{
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "更改名称")
                            }
                        }
                    )
                }

                // 保存名称



                Spacer(modifier = Modifier.height(16.dp))

                // 限速设置
                Column {

                    Text("下载限速: ${if (speedLimitRange == 0F) "不限速" else "${convertBinaryType(value = (speedLimitRange * (50 * BinaryType.MB.size)).toLong())}/s"}")
                    Slider(
                        value = speedLimitRange,
                        onValueChange = { speedLimitRange = it },
                        valueRange = 0f..1f, // 0-50MB/s
                        steps = 20
                    )
                }

//                Spacer(modifier = Modifier.height(8.dp))
//
//                // 线程数设置
//                Column {
//                    Text("下载线程：$threadCount")
//                    Slider(
//                        value = threadCount.toFloat(),
//                        onValueChange = { threadCount = it.toInt() },
//                        valueRange = 1f..10f,
//                        steps = 8
//                    )
//                }

                Spacer(modifier = Modifier.height(16.dp))


            }




        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
fun DownloadProgressBar(
    downloadTask: DownloadTask,
    chunkProgress: List<Int>?,
){

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(25.dp)
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = RectangleShape
            )

    ){

        Row{

            chunkProgress?.let{ chunkProgress->
                if(chunkProgress.all { it == -1 }){
                    Text("Finished")
                }

                val fileSize = downloadTask.fileSize

                List(chunkProgress.size){ chunkIndex ->

                    val currentChunkDownloadedSize = chunkProgress[chunkIndex]
                    val currentChunkSize = (downloadTask.taskInformation.chunksRangeList[chunkIndex].second - downloadTask.taskInformation.chunksRangeList[chunkIndex].first) + 1
                    val currentChunkRatio = currentChunkDownloadedSize.toFloat() / currentChunkSize

                    Box(
                        modifier = Modifier
                            .height(25.dp)
                            .weight(
                                weight = currentChunkSize.toFloat() / fileSize,
                            )

                    ){

                        Box(
                            modifier = Modifier
                                .height(25.dp)
                                .fillMaxWidth( fraction = currentChunkRatio )
                                .background(color = Color.LightGray)

                        )


                    }


                }

            }




        }

        chunkProgress?.let{ chunkProgress ->
            val currentProgress = chunkProgress.sum().toFloat()/downloadTask.fileSize
            Text(
                "${String.format("%.2f", currentProgress*100)}%"
            )
        }



    }



}