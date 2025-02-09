package com.example.kotlinstart.models

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile

import java.io.File

enum class UriAuthority(val authority: String) {
    ExternalStorageDocument("com.android.externalstorage.documents"),
    DownloadsDocument("com.android.providers.downloads.documents"),
    MediaDocument("com.android.providers.media.documents")
}


object StorageModel {

        var grantedSavePath:Uri? = null

        fun initModel(context: Context): Boolean{
            val permissionList = context.contentResolver.persistedUriPermissions

            if(permissionList.isEmpty()){
               return false
            }

            else{
                grantedSavePath = permissionList.first().uri
                Log.d("test","StorageModel.grantedSavePath:${grantedSavePath}")
                return true
            }
        }


        fun getPathFromUri(
            uri: Uri?,
            context: Context,
        ): String? {

            if (uri == null) return null

            if (DocumentsContract.isDocumentUri(context, uri) ) {
                // 处理DocumentProvider类型的Uri
//            即 /document/document:1000047369 这种的uri

                if (isExternalStorageDocument(uri)) {
                    val docId: String = DocumentsContract.getDocumentId(uri)
                    val split: Array<String?> =
                        docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        return "${Environment.getExternalStorageDirectory()}/${split[1]}"
                    }
                }

                else if (isDownloadsDocument(uri)) {
                    val id: String = DocumentsContract.getDocumentId(uri)
                    val contentUri: Uri? = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), //???
                        id.toLong()
                    )
                    return getDataColumn(context, contentUri, null, null)
                }

                else if (isMediaDocument(uri)) {
                    val docId: String = DocumentsContract.getDocumentId(uri)
                    val split: Array<String?> =
                        docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null

                    when(type){
                        "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }

                    val selection = "_id=?"
                    val selectionArgs = arrayOf<String?>(split[1])
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
            }

            else if ("content".equals(uri.scheme, ignoreCase = true)) {
                // 处理ContentProvider类型的Uri
                return getDataColumn(context, uri, null, null)
            }

            else if ("file".equals(uri.scheme, ignoreCase = true)) {
                // 处理File类型的Uri
                return uri.path
            }

            return null
        }

        private fun getDataColumn(
            context: Context,
            uri: Uri?,
            selection: String? = null,
            selectionArgs: Array<String?>? = null,
            sortOrder: String? = null,
        ): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf<String?>(column)

            try {


                cursor =
                    uri?.let {
                        context.contentResolver.query(it, projection, selection, selectionArgs, sortOrder)
                    }

                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex: Int = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(columnIndex)
                }
            }

            finally {
                cursor?.close()
            }
            return null
        }

        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return uri.authority == UriAuthority.ExternalStorageDocument.authority
        }

        private fun isDownloadsDocument(uri: Uri): Boolean {
            return uri.authority == UriAuthority.DownloadsDocument.authority
        }

        private fun isMediaDocument(uri: Uri): Boolean {
            return uri.authority == UriAuthority.MediaDocument.authority
        }

        fun copyFileToSAFDirectory(
            context: Context,
            saveDirectoryUri: Uri?,
            sourceFile: File?,
            targetFileName: String,
        ) {

            if(sourceFile == null || saveDirectoryUri == null) {
                //may toast show what's wrong
                return
            }

            try {
                // 创建目标文件的 Uri
                //Way 1 DocumentTreeFile => DocumentFile
                //
//                val saveDirectoryFile = DocumentFile.fromTreeUri(context, saveDirectoryUri)
//                val targetFile = saveDirectoryFile?.createFile(mimeType, targetFileName)

                val mimeType = getMimeType(sourceFile)

                //Way 2 DocumentTreeFile => (TreeId)DocumentId => DocumentUri
                val docID = DocumentsContract.getTreeDocumentId(saveDirectoryUri)
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(saveDirectoryUri, docID)


                Log.d("test","parentUri: ${parentUri?.path} / id: $docID")

                val targetFile = DocumentsContract.createDocument(
                    context.contentResolver,
                    parentUri,
                    mimeType,
                    targetFileName
                )


                Log.d("test","testUri: ${targetFile?.path}")


                targetFile?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        // 打开源文件的输入流
                        sourceFile.inputStream().use { inputStream ->
                            // 将源文件数据写入目标 Uri
                            inputStream.copyTo(outputStream)
                        }
                        Log.d("test","文件写入成功: $targetFileName")
                    }
                }

            }

            catch (e: Exception) {
                e.printStackTrace()
                Log.d("test","文件写入失败:${e.message}")
                // Invalid URI error
            }
        }

        fun getMimeType(file: File): String {
            val extension = file.extension
            return when (extension.lowercase()) {
                "txt" -> "text/plain"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "mp4" -> "video/mp4"
                else -> "application/octet-stream"
            }
        }


}
