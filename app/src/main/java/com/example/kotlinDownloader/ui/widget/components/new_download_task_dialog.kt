package com.example.kotlinDownloader.ui.widget.components

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.datastore.preferences.core.edit
import androidx.documentfile.provider.DocumentFile
import com.example.kotlinDownloader.internal.AppConfig

import com.example.kotlinDownloader.internal.DownloadTask
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager
import com.example.kotlinDownloader.internal.PaddingH6
import com.example.kotlinDownloader.internal.PaddingV12
import com.example.kotlinDownloader.internal.PaddingV16
import com.example.kotlinDownloader.internal.PaddingV6
import com.example.kotlinDownloader.internal.TaskInformation
import com.example.kotlinDownloader.internal.android.DirectoryLauncher
import com.example.kotlinDownloader.internal.android.judgePermissionUri
import com.example.kotlinDownloader.internal.convertBinaryType
import com.example.kotlinDownloader.internal.convertDocUri
import com.example.kotlinDownloader.internal.convertSpeedLimit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun AddTaskDialog(
    linkUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (DownloadTask,Int) -> Unit, // 最终确认回调
    threadCount: Int = 5,
    defaultStoragePath: Uri? = null,
) {

    var taskInformation by remember { mutableStateOf(TaskInformation.Default) }

    var url by remember { mutableStateOf(linkUrl) }
    var storagePath by remember { mutableStateOf(defaultStoragePath) }

    var speedLimitRange by remember { mutableFloatStateOf(0F) } // 0 表示不限速
    var threadCount by remember { mutableIntStateOf(threadCount) }

    var isAutoNamed by remember { mutableStateOf(true) } // 标记是否自动命名
    var isRequested by remember { mutableStateOf(false) } // 请求是否已加载
    var requestServerFileName by remember { mutableStateOf(TaskInformation.Default.fileName) } // 标记是否自动命名

    val localContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectStorageDirectoryLauncher = DirectoryLauncher(
        context = localContext,
    ){
        storagePath = DocumentFile.fromTreeUri(localContext, it)?.uri

        coroutineScope.launch {
            AppConfig.updateAppConfig(
                localContext,
                AppConfig.appConfigs.copy(
                    storagePath.toString()
                )
            )

        }
    }

    // 自动获取文件名逻辑 每当url改变时触发
    LaunchedEffect(url) {
        isRequested = false
        if (url.isNotEmpty() && isAutoNamed) {

            taskInformation = taskInformation.copy(
                downloadUrl = url,
                fileName = URL(url).path.substringAfterLast('/'), //率先名字
                taskID = url.hashCode().toString(),
            )

            //请求数据之后 得出的服务器名称
                try {
                    withContext(Dispatchers.IO) {
                        val requestTaskInformation = MultiThreadDownloadManager.getFileInformation(url)
//                        fileSize = requestTaskInformation.fileSize
                        requestServerFileName = requestTaskInformation.fileName

                        taskInformation = requestTaskInformation.copy(
                            fileName = taskInformation.fileName
                        )

                        isRequested = true
                        // TODO 预计行为是HEAD请求获取size与name 否则就默认采用后缀名
                    }
            }

            catch (e: Exception) {
                Log.d("taskDialog","未知文件")
                ""
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {

                    Text(
                        text = "添加任务...",
                        style = TextStyle(fontSize = 24.sp)
                    )

                    PaddingV16()

                    // 下载链接
                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            isAutoNamed = true // 重置自动命名标记
                        },
                        label = { Text("下载链接") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    PaddingV6()

                    // 保存名称
                    OutlinedTextField(
                        value =
                            if(taskInformation.fileName == TaskInformation.Default.fileName) ""
                            else taskInformation.fileName
                        ,
                        onValueChange = {

                            taskInformation = taskInformation.copy(
                                fileName = it
                            )
                            isAutoNamed = false
                        },
                        label = { Text("保存名称") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    PaddingV6()

                    // 存储目录
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        OutlinedTextField(
                            value = storagePath?.path ?: "",
                            label = { Text("存储目录") },
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            onValueChange = {},
                        )

                        IconButton(
                            onClick = { selectStorageDirectoryLauncher.launch(null) }
                        ) {
                            Icon(Icons.Default.Edit, "选择目录")
                        }
                    }

                    PaddingV6()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ){
                        Text("文件大小: ${if (taskInformation.fileSize == 0L) "未知" else convertBinaryType(value = taskInformation.fileSize)}")

                        if(!isRequested && url.isNotEmpty()){
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = requestServerFileName != TaskInformation.Default.fileName,
                        enter = fadeIn() + expandIn(),
                        exit = shrinkOut() + fadeOut(),
                    ){
                        Column {
                            PaddingV6()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        taskInformation = taskInformation.copy(
                                            fileName = requestServerFileName
                                        )
                                    }
                            ){
                                Text("服务端获取的文件名: $requestServerFileName")
                            }

                            PaddingV6()
                        }


                    }

                    PaddingV12()

                    // 限速设置
                    Column {
                        Text("下载限速: ${if (speedLimitRange == 0F) "不限速" else "${convertBinaryType(value = convertSpeedLimit(speedLimitRange))}/s"}")
                        Slider(
                            value = speedLimitRange,
                            onValueChange = { speedLimitRange = it },
                            valueRange = 0f..1f, // 0-20MB/s
                        )
                    }

                    PaddingV12()

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

                    PaddingV12()

                    // 操作按钮
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) { Text("取消") }

                        PaddingH6()

                        TextButton(
                            onClick = {
                                val permissionList = localContext.contentResolver.persistedUriPermissions

                                Log.d("storagePath","permissionList:$permissionList")

                                storagePath?.let { storagePath ->
                                    if(storagePath.path!!.isNotEmpty()){

                                        //granted检测
                                        if(judgePermissionUri(permissionList, storagePath)){

                                            //拼接 documentTreeUri 为 documentUri

                                            val targetFileUri = convertDocUri(
                                                context = localContext,
                                                storagePath = storagePath,
                                                fileName = taskInformation.fileName
                                            )

                                            targetFileUri?.let{

                                                onConfirm(
                                                    DownloadTask(
                                                        taskInformation = taskInformation.copy(
                                                            storagePath = targetFileUri.toString()
                                                        ),
                                                        speedLimit = convertSpeedLimit(speedLimitRange),
                                                    ),
                                                    threadCount

                                                )
                                            }

                                        }

                                        else{
                                            //  TODO not granted toaster
                                            Log.d("TaskInfo","该目录没有授权,请重新选择该目录进行授权")
                                            Log.d("TaskInfo","已授权目录: $permissionList")

                                        }

                                    }

                                }

                                onDismiss()
                            }
                        ) {
                            Text("确认")
                        }
                    }
                }
            }
        }
}

