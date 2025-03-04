package com.example.kotlinDownloader.internal

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.Long


fun convertIconState(taskStatusState: TaskStatus?): ImageVector{
    var iconStatus = Icons.Default.Done

    when(taskStatusState){
        TaskStatus.Pending -> iconStatus = Icons.Default.History
        TaskStatus.Activating -> iconStatus = Icons.Default.Pause
        TaskStatus.Paused -> iconStatus = Icons.Default.PlayArrow
        TaskStatus.Finished -> iconStatus = Icons.Default.Done
        TaskStatus.Stopped -> iconStatus = Icons.Default.Stop
        null -> { }
    }

    return iconStatus
}

fun convertBinaryType(value: Long): String{
    var resultType = BinaryType.B.binaryType
    var resultValue = value.toFloat()

    BinaryType.entries.any {
        //1000*1024 => 1000 KB => 0.97MB

        if(resultValue<1024) {
            //划分之后 如果数值超过 1000 那么应该再切割一下 并更新Type
            resultType = it.binaryType

            if(resultValue>=1000){
                resultValue /= BinaryType.KB.size
                return@any false
            }

            else{
                return@any true
            }

        }

        else{
            resultValue /= BinaryType.KB.size
        }

        return@any false



    }


    var splitResult = resultValue.toString().split(".")

    if(splitResult.last().length <= 2){
        return "$resultValue$resultType"
    }

    return "${splitResult[0]}.${splitResult[1].substring(0,2)}$resultType"


}

fun convertDocUri(
    context: Context,
    storagePath: Uri?,
    fileName: String
): Uri? {
    val docID = DocumentsContract.getTreeDocumentId(storagePath)
    val parentUri = DocumentsContract.buildDocumentUriUsingTree(storagePath, docID)

    return DocumentsContract.createDocument(
        context.contentResolver,
        parentUri,
        "", //mimeType
        fileName
    )
}

fun convertSpeedLimit(
    speedLimitRange: Float,
    maxValue: Long = (20 * BinaryType.MB.size),
): Long = (speedLimitRange * maxValue).toLong()

fun convertChunkSize(
    chunksRange: Pair<Long, Long>,
): Long = chunksRange.second - chunksRange.first + 1

fun convertDownloadedSize(
    chunksRangeList: List<Pair<Long, Long>>,
    chunkProgress: List<Int>
): Long{

    var totalDownloaded = 0L

    chunkProgress.mapIndexed { chunkIndex,currentChunkProgress ->
        if( currentChunkProgress == chunkFinished ){
            totalDownloaded += convertChunkSize(chunksRangeList[chunkIndex])
        }

        else{
            totalDownloaded += currentChunkProgress
        }
    }

    return totalDownloaded
}