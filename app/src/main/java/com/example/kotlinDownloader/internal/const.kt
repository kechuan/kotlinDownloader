package com.example.kotlinDownloader.internal

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

const val chunkFinished = -1
val emptyLength: Long? = null

val Padding6 = @Composable { Spacer(modifier = Modifier.padding(6.dp)) }
val Padding12 = @Composable { Spacer(modifier = Modifier.padding(12.dp)) }
val PaddingH6 = @Composable { Spacer(modifier = Modifier.padding(horizontal = 6.dp)) }
val PaddingV6 = @Composable { Spacer(modifier = Modifier.padding(vertical = 6.dp)) }
val PaddingH12 = @Composable { Spacer(modifier = Modifier.padding(horizontal = 12.dp)) }
val PaddingV12 = @Composable { Spacer(modifier = Modifier.padding(vertical = 12.dp)) }
val PaddingH16 = @Composable { Spacer(modifier = Modifier.padding(horizontal = 16.dp)) }
val PaddingV16 = @Composable { Spacer(modifier = Modifier.padding(vertical = 16.dp)) }

enum class BinaryType(
    val binaryType: String,
    val size: Long
){
    B("B",0),
    KB("KB",1*1024),
    MB("MB",1*1024*1024),
    GB("GB",1*1024*1024*1024)
}

@Serializable
enum class TaskStatus{
    Pending,
    Activating,
    Finished,
    Paused,
    Stopped
}