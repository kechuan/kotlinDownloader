import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField

import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

import com.example.kotlinDownloader.internal.MultiThreadDownloadManager.findTask

@Composable
fun RenameTaskDialog(
    taskID: String,
    onDismiss: () -> Unit,
    onConfirm: (String,String) -> Unit, // 最终确认回调

) {

    var fileName by remember { mutableStateOf(findTask(taskID)?.taskInformation?.fileName) }

    fileName?.let{
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
                        modifier = Modifier.padding(bottom = 16.dp),
                        text = "重命名",
                        style = TextStyle(fontSize = 24.sp)
                    )

                    // 下载链接
                    TextField(
                        value = fileName!!,
                        onValueChange = {
                            fileName = it
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 操作按钮
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(
                            onClick = {
                                if(fileName == null) return@TextButton
                                onConfirm(taskID,fileName!!)
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

