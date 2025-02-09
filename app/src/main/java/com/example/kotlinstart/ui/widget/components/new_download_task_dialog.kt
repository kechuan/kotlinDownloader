import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun AddTaskDialog(
    linkUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (DownloadTask) -> Unit, // 最终确认回调
    defaultSavePath: String,
    dialogStatus: Boolean
) {
    var url by remember { mutableStateOf(linkUrl) }
    var fileName by remember { mutableStateOf("") }
    var storagePath by remember { mutableStateOf(defaultSavePath) }
    var speedLimitRange by remember { mutableStateOf(0F) } // 0 表示不限速
    var threadCount by remember { mutableStateOf(4) }
    var isAutoNamed by remember { mutableStateOf(true) } // 标记是否自动命名

    val localContext = LocalContext.current

    // 文件选择器
    val directoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            storagePath = DocumentFile.fromTreeUri(localContext, it)?.name ?: storagePath
        }
    }

    // 自动获取文件名逻辑
    LaunchedEffect(dialogStatus, url) {
        if (dialogStatus && url.isNotEmpty() && isAutoNamed) {
            fileName = try {
                // 模拟网络请求获取文件名
                withContext(Dispatchers.IO) {
                    delay(500) // 模拟网络延迟
                    URL(url).path.substringAfterLast('/')
                }
            }

            catch (e: Exception) {
                "未知文件"
            }
        }
    }

    if (dialogStatus) {
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // 保存名称
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = {
                            fileName = it
                            isAutoNamed = false // 用户手动修改后关闭自动命名
                        },
                        label = { Text("保存名称") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 存储目录
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = storagePath,
                            onValueChange = { storagePath = it },
                            label = { Text("存储目录") },
                            modifier = Modifier.weight(1f),
                            readOnly = true
                        )
                        IconButton(onClick = { directoryLauncher.launch(null) }) {
                            Icon(Icons.Default.Add, "选择目录")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 限速设置
                    Column {

                        Text("下载限速: ${if (speedLimitRange == 0F) "不限速" else "${convertBinaryType(value = (speedLimitRange*50/20).toLong())}MB/s"}")
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

                    // 操作按钮
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            onConfirm(
                                DownloadTask(
                                    taskName = fileName,
                                    taskID = url.hashCode().toString(),
                                    downloadUrl = url,
                                    storagePath = storagePath,
                                    speedLimit = 12*BinaryType.MB.size,
                                    threadCount = threadCount,
//                                    progressCallback = {
////                                        DownloadViewModel.updateTaskProgress(fileName,)
//                                    }
                                )
                            )
                            onDismiss()
                        }) {
                            Text("确认")
                        }
                    }
                }
            }
        }
    }
}

