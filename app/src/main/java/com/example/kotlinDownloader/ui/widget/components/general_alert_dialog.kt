package com.example.kotlinDownloader.ui.widget.components

import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager.findTask
import com.example.kotlinDownloader.internal.PaddingV12
import com.example.kotlinDownloader.internal.PaddingV6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralAlertDialog(
    title:String = "title",
    subTitle: String = "subTitle",
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {}, // 最终确认回调
) {


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
                    text = title,
                    style = TextStyle(fontSize = 24.sp)
                )

                PaddingV6()

                Text(subTitle, style = TextStyle(fontSize = 16.sp))

                PaddingV6()

                // 操作按钮
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            onConfirm()
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

