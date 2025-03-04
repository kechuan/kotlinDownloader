package com.example.kotlinDownloader.internal.android

import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
fun DirectoryLauncher(
    context: Context,
    directorySelectedCallback: (Uri) -> Unit = {},
): ManagedActivityResultLauncher<Uri?, Uri?> {
    // 创建启动器来启动目录选择器，并处理结果 一般是用作写入用途
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {

            val takeFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.contentResolver
                .takePersistableUriPermission(it, takeFlags)

            directorySelectedCallback(uri)


        } ?: println("No directory selected.")
    }
}

@Composable
fun FilePickerLauncher(
    fileSelectedCallback: (Uri?) -> Unit = {},
): ManagedActivityResultLauncher<Array<String>, Uri?>{
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { fileSelectedCallback }
    )
}

// 提取并标准化 Document ID
fun getNormalizedDocumentId(uri: Uri): String {
    return if (DocumentsContract.isTreeUri(uri)) Uri.decode(DocumentsContract.getTreeDocumentId(uri))
           else Uri.decode(DocumentsContract.getDocumentId(uri))
}

// 比较两个 URI 是否指向同一资源或其子项
fun isSameOrChildUri(parentUri: Uri?, sonUri: Uri?): Boolean {
    val id1 = getNormalizedDocumentId(parentUri ?: return false)
    val id2 = getNormalizedDocumentId(sonUri ?: return false)

    // 判断是否完全相同，或是否为父子关系
    return id1 == id2 || id2.startsWith("$id1/")
}

fun judgePermissionUri(
    permissionUrisList :List<UriPermission?>,
    storagePath:Uri,
): Boolean{
    return permissionUrisList.any{
        isSameOrChildUri(storagePath, it?.uri) // 返回 true
    }
}

