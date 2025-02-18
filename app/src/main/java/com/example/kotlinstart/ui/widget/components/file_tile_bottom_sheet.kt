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

import com.example.kotlinstart.internal.DownloadTask


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTileBottomSheet(
    selectingTask: DownloadTask,
    onDismiss: () -> Unit = {},
) {

    val localContext = LocalContext.current



    //Info
    var url by remember { mutableStateOf(selectingTask.taskInformation.downloadUrl) } //刷新下载地址??
    var fileName by remember { mutableStateOf(selectingTask.taskInformation.taskName) }
//    val fileName = selectingTask.taskInformation.taskName

    //(speedLimitRange*(50*BinaryType.MB.size)).toLong()

    //settingPanel
    var speedLimitRange by remember { mutableFloatStateOf((selectingTask.speedLimit.toFloat())/(50*BinaryType.MB.size)) } // 0 表示不限速
    var threadCount by remember { mutableIntStateOf(selectingTask.threadCount) }




    //期望: Column -> taskName/(Info)chunkProgress + basicAction[/Pause/resume/Delete]/(control)
    //TabRows(overview/Details)
    ModalBottomSheet(
        onDismissRequest = onDismiss
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Text(text = fileName)

                    Text(text = selectingTask.taskStatus.name)
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider()

                //chunkProgress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()

                ){
                    Text(

                        text = "should ready for chunk Progress",
//                            color = Color.RED
                    )
                }


                Column {

                    ListItem(headlineContent = {
                        Row{
//                                Icon(painter = Icons.Default.Paused, contentDescription = "Pause")

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(text = "暂停")

                        }
                    })

                    Spacer(modifier = Modifier.height(16.dp))

                    ListItem(

                        modifier = Modifier.clickable(

                            onClick = {
                                MultiThreadDownloadManager.removeTask(
                                    context = localContext,
                                    taskID = selectingTask.taskInformation.taskID,
                                )
                            }
                        ),

                        headlineContent = {
                            Row{
                                //                                Icon(painter = Icons.Default.Paused, contentDescription = "Pause")

                                Spacer(modifier = Modifier.width(16.dp))

                                Text(text = "删除")

                            }
                        })
                }

                // 保存名称
                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it

                    },
                    label = { Text("保存名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 限速设置
                Column {

                    Text("下载限速: ${if (speedLimitRange == 0F) "不限速" else "${convertBinaryType(value = (speedLimitRange*(50*BinaryType.MB.size)).toLong())}/s"}")
                    Slider(
                        value = speedLimitRange,
                        onValueChange = { speedLimitRange = it },
                        valueRange = 0f..1f, // 0-50MB/s
                        steps = 20
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 线程数设置
                Column {
                    Text("下载线程：$threadCount")
                    Slider(
                        value = threadCount.toFloat(),
                        onValueChange = { threadCount = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))


            }




        }
    }
}


