import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun FileTile(
    taskName: String,
    progress: Float,
    totalSize: Long
){

    var multiCheckStatus by remember { mutableStateOf(false) }
    var lastProgress = 0.0F


    ListItem(

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
                            text = "speed: ${convertBinaryType(totalSize*(progress - lastProgress).toLong())}",
                            style = TextStyle(color = Color.Black)
                        ).run {
                            lastProgress = progress
                        }

                        Text(text = "size: ${if(totalSize == 0L) "正在查询信息..." else convertBinaryType(totalSize) }")
                    }



                }
            }
        },

        trailingContent = {
//            Checkbox(checked = multiCheckStatus, onCheckedChange = {multiCheckStatus=!multiCheckStatus} )
        },
    )
}

