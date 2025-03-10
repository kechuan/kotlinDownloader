
import android.annotation.SuppressLint
import android.util.Log
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


import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.filled.Search
import com.example.kotlinDownloader.internal.BinaryType

import com.example.kotlinDownloader.internal.DownloadTask
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager.downloadingTaskFlow
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager.updateTaskSpeedLimit
import com.example.kotlinDownloader.internal.PaddingV6
import com.example.kotlinDownloader.internal.TaskStatus
import com.example.kotlinDownloader.internal.chunkFinished
import com.example.kotlinDownloader.internal.convertBinaryType
import com.example.kotlinDownloader.internal.convertChunkSize
import com.example.kotlinDownloader.internal.convertDownloadedSize
import com.example.kotlinDownloader.internal.convertIconState
import com.example.kotlinDownloader.internal.convertSpeedLimit

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTileBottomSheet(
    selectingTask: DownloadTask,
    renameAction: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {

    val localContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    //这两个活动流 都是以 在下载中 为前提的
    //如果寻找不到 则统一被当作为 已被移入到 FinishedTaskFlow 也就是 结果为 null
    val chunkProgress by downloadingTaskFlow
        .map {
            it.firstOrNull { it.taskInformation.taskID == selectingTask.taskInformation.taskID }?.chunkProgress
        }
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
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = TaskStatus.Finished
        )
        .collectAsState()

    var fileName by remember { mutableStateOf(selectingTask.taskInformation.fileName) }
    var speedLimitRange by remember { mutableFloatStateOf((selectingTask.speedLimit.toFloat())/(20 * BinaryType.MB.size)) } // 0 表示不限速



    //期望: Column -> fileName/(Info)chunkProgress + basicAction[/Pause/resume/Delete]/(control)
    //TabRows(overview/Details)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = {},
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
                            imageVector = convertIconState(currentTaskStatus ?: TaskStatus.Finished),
                            contentDescription = currentTaskStatus?.name ?: TaskStatus.Finished.name,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text = currentTaskStatus?.name ?: TaskStatus.Finished.name)
                    }

                }

                Spacer(modifier = Modifier.height(16.dp))

                DownloadProgressBar(
                    downloadTask = selectingTask,
                    chunkProgress = chunkProgress,
                )

                //Action Area
                Column {

                    val targetStatus =
                        if(currentTaskStatus == TaskStatus.Activating) TaskStatus.Paused
                        else TaskStatus.Activating

                    if(currentTaskStatus != null) {
                        ListItem(
                            modifier = Modifier
                                .clickable(
                                    onClick = {

                                        if (currentTaskStatus == TaskStatus.Finished) return@clickable

                                        coroutineScope.launch {
                                            MultiThreadDownloadManager.updateTaskStatus(
                                                context = localContext,
                                                taskID = selectingTask.taskInformation.taskID,
                                                taskStatus = targetStatus
                                            )
                                        }.run {
                                            if(targetStatus == TaskStatus.Activating){
                                                MultiThreadDownloadManager.addTask(
                                                    context = localContext,
                                                    downloadTask = selectingTask,
                                                    threadCount = selectingTask.chunkProgress.size,
                                                )
                                            }
                                        }

                                    }
                                ),

                            headlineContent = {

                                if (currentTaskStatus == TaskStatus.Activating) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause")
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(text = "暂停")

                                    }
                                } else {

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume")
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(text = "恢复")

                                    }


                                }


                            }
                        )
                    }


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
                                onClick = { renameAction() }
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

                    HorizontalDivider()

                    ListItem(
                        modifier = Modifier
                            .clickable(
                                onClick = {
                                    Log.d("taskUI","current Limit:${MultiThreadDownloadManager.findTask(selectingTask.taskInformation.taskID)?.speedLimit}")
                                }
                            )
                        ,
                        headlineContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Look up")
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "(Test) 显示数据")
                            }
                        }
                    )

                }

                // 保存名称
                Spacer(modifier = Modifier.height(16.dp))

                // 限速设置
                Column {

                    if(currentTaskStatus != null){
                        Column{
                            Text("下载限速: ${if (speedLimitRange == 0F) "不限速" else "${convertBinaryType(value = convertSpeedLimit(speedLimitRange) )}/s"}")
                            PaddingV6()
                            Slider(
                                value = speedLimitRange,
                                onValueChange = { speedLimitRange = it },
                                onValueChangeFinished = {
                                    updateTaskSpeedLimit(
                                        taskID = selectingTask.taskInformation.taskID,
                                        speedLimit = convertSpeedLimit(speedLimitRange)
                                    )
                                },
                                valueRange = 0f..1f, // 0-20MB/s
                            )
                        }
                    }

                }


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

        //in Download Progress
        Box {
            Row {

                chunkProgress?.let { chunkProgress ->

                    if (chunkProgress.all { it == chunkFinished }) {
                        Text("Finished")
                    }

                    val fileSize = downloadTask.taskInformation.fileSize

                    List(chunkProgress.size) { chunkIndex ->

                        val currentChunkSize =
                            convertChunkSize(downloadTask.taskInformation.chunksRangeList[chunkIndex])

                        val currentChunkDownloadedSize =
                            if(chunkProgress[chunkIndex] == chunkFinished) currentChunkSize.toInt()
                            else chunkProgress[chunkIndex]

                        val currentChunkRatio = currentChunkDownloadedSize.toFloat() / currentChunkSize


                        Box(
                            modifier = Modifier
                                .height(25.dp)
                                .weight(
                                    weight = currentChunkSize.toFloat() / fileSize,
                                )

                        ) {

                            Box(
                                modifier = Modifier
                                    .height(25.dp)
                                    .fillMaxWidth(fraction = currentChunkRatio)
                                    .background(color = Color.LightGray)

                            )


                        }


                    }

                }

            }

            chunkProgress?.let{ chunkProgress ->


                val totalDownloaded = convertDownloadedSize(
                    chunksRangeList = downloadTask.taskInformation.chunksRangeList,
                    chunkProgress = chunkProgress
                )

                val currentProgress = totalDownloaded.toFloat()/downloadTask.taskInformation.fileSize

                Box(
                    modifier = Modifier
                        .height(25.dp)
                        .fillMaxWidth()
                ){

                    if(currentProgress == 0.0F){
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = TaskStatus.Pending.name
                        )
                    }

                    else{
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = "${String.format("%.2f", currentProgress*100)}%"
                        )
                    }


                }


            }
        }

        //not found => Finished
        if(chunkProgress == null){
            Box(
                modifier = Modifier
                    .height(25.dp)
                    .fillMaxWidth()
                    .background(color = Color.LightGray)

            ){
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = TaskStatus.Finished.name
                )
            }


        }



    }



}