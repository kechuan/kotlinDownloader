import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
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
import java.io.File
import java.net.URL

@Composable
fun AddTaskDialog(
    linkUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (DownloadTask) -> Unit, // 最终确认回调
    defaultStoragePath: Uri? = null,
    dialogStatus: Boolean
) {
    var url by remember { mutableStateOf(linkUrl) }
    var fileName by remember { mutableStateOf("") }
    var storagePath by remember { mutableStateOf(defaultStoragePath) }
    var speedLimitRange by remember { mutableFloatStateOf(0F) } // 0 表示不限速
    var threadCount by remember { mutableIntStateOf(1) }
    var isAutoNamed by remember { mutableStateOf(true) } // 标记是否自动命名

    val localContext = LocalContext.current

    // 文件选择器
    val directoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {

            val takeFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            localContext.contentResolver
                .takePersistableUriPermission(it, takeFlags)

            storagePath = DocumentFile.fromTreeUri(localContext, it)?.uri

        } ?: println("No directory selected.")
    }


    // 自动获取文件名逻辑
    LaunchedEffect(dialogStatus, url) {
        if (dialogStatus && url.isNotEmpty() && isAutoNamed) {
            fileName = try {
                // 模拟网络请求获取文件名
                withContext(Dispatchers.IO) {
                    delay(500) // 模拟网络延迟
                    // TODO 预计行为是HEAD请求获取size与name 否则就默认采用后缀名
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
                            value = storagePath?.path ?: "",
//                            onValueChange = { storagePath = it }, URI问题
                            onValueChange = {  },
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

//                            val storagePathHash:File = File(storagePath)
//
//                            // TODO 封禁情况
//                            // 1.isDirectory
//                            // 2.not granted path

                            storagePath?.let { storagePath ->
                                if(storagePath.path!!.isNotEmpty()){

                                    //granted检测
                                    val permissionList = localContext.contentResolver.persistedUriPermissions

//                                    if(permissionList.any{ it.uri == storagePath }){

                                        //拼接 documentTreeUri 为 documentUri

                                        val docID = DocumentsContract.getTreeDocumentId(storagePath)
                                        val parentUri = DocumentsContract.buildDocumentUriUsingTree(storagePath, docID)


                                        val targetFile = DocumentsContract.createDocument(
                                            localContext.contentResolver,
                                            parentUri,
                                            "",
                                            fileName
                                        )

                                        Log.d("confirm Task","storageUri: $targetFile")

                                        onConfirm(

                                            DownloadTask(
                                                taskName = fileName,
                                                taskID = url.hashCode().toString(), //TODO 相同ID处理机制When..
                                                downloadUrl = url,
//                                                storagePath = storagePath,
                                                storagePath = targetFile!!,
                                                speedLimit = (speedLimitRange*(50*BinaryType.MB.size)).toLong(),
                                                threadCount = threadCount,
                                            ),



                                        )
//                                    }

//                                    else{
                                        //TODO not granted toaster
//                                        println("该目录没有授权,请重新选择该目录进行授权")
//                                        println("已授权目录: $permissionList")
//                                    }

                                }

                            }
//


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

