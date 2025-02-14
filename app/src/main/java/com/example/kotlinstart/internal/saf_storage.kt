import android.content.Context
import android.content.UriPermission
import android.net.Uri
import android.provider.DocumentsContract

//fun dirPickerBuild(
//    context:Context,
//){
//    val launcher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
//        uri?.let {
//            // 持久化访问权限配置 和 intent是一致的行为
//            val takeFlags =
//                Intent.FLAG_GRANT_READ_URI_PERMISSION or
//                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//
//            context.contentResolver
//                .takePersistableUriPermission(it, takeFlags)
//
//            grantedUri = it
//
//
//            println("Selected directory URI: $it")
//
//
//
//        } ?: println("No directory selected.")
//
//        launcher(null)
//}

// 提取并标准化 Document ID
fun getNormalizedDocumentId(uri: Uri): String {
    return if (DocumentsContract.isTreeUri(uri)) {
        Uri.decode(DocumentsContract.getTreeDocumentId(uri))
    } else {
        Uri.decode(DocumentsContract.getDocumentId(uri))
    }
}

// 比较两个 URI 是否指向同一资源或其子项
fun isSameOrChildUri(uri1: Uri?, uri2: Uri?): Boolean {
    val id1 = getNormalizedDocumentId(uri1 ?: return false)
    val id2 = getNormalizedDocumentId(uri2 ?: return false)

    // 判断是否完全相同，或是否为父子关系
    return id1 == id2 || id2.startsWith("$id1/")
}

// 使用示例
//val uriFromPersisted = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload%2FkotlinStartPicture")
//val uriFromContract = Uri.parse("content://com.android.externalstorage.documents/tree/primary:Download/kotlinStartPicture/document/primary:Download/kotlinStartPicture")
//
//val isSame = isSameOrChildUri(uriFromPersisted, uriFromContract) // 返回 true

fun judgePermissionUri(
    permissionUrisList :List<UriPermission?>,
    storagePath:Uri,
): Boolean{
    return permissionUrisList.any{
        isSameOrChildUri(storagePath, it?.uri) // 返回 true
    }
}

fun convertDocumentTree(uri:Uri){

}