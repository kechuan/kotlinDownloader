package com.example.kotlinstart.ui.widget.catalogs.downloadPage

import AddTaskDialog

import FileTile
import FileTileBottomSheet
import MultiThreadDownloadManager.preallocateSpace
import com.example.kotlinstart.internal.DownloadTask
import com.example.kotlinstart.internal.TaskInformation
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
//import NewDownloadTaskDialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings

import androidx.compose.material3.*
import androidx.compose.runtime.*


import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.example.kotlinstart.ui.widget.catalogs.DownloadPageTabs
//import com.example.kotlinstart.MultiThreadDownloadManager
import com.example.kotlinstart.ui.widget.catalogs.DownloadRoutes

import kotlinx.coroutines.flow.collectLatest

import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.IOException

@Composable
fun DownloadTaskPage(){
    val parentNavController = MainViewModel.localNavController.current
    val downloadNavController = DownloadViewModel.localDownloadNavController.current

    val localContext = LocalContext.current

    var newTaskDialogStatus by remember { mutableStateOf(false) }
    var selectingTile by remember{ mutableStateOf(DownloadTask()) }



    if(newTaskDialogStatus) {
        AddTaskDialog(
            linkUrl = "", //预计读取剪切板
            onDismiss = { newTaskDialogStatus = false },
            onConfirm = { downloadTask ->

                MultiThreadDownloadManager.addTask(
                    context = localContext,
                    downloadTask = downloadTask
                )

                println("result:$downloadTask")
            },
            defaultStoragePath = null,
            dialogStatus = newTaskDialogStatus

        )
    }

    if(selectingTile.taskInformation.taskID != "taskID"){
        FileTileBottomSheet(
            selectingTask = selectingTile,
            onDismiss = {
                selectingTile = DownloadTask()
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {

            FlutterDesignWidget.AppBar(
                onClick = {
                    parentNavController.popBackStack()
                },
                title = { Text("DownloadPage") },
                actions = {

                    var toolbarExpandedStatus by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = {

                        }
                    ) {
                        Icon(Icons.Filled.Check, "Trigger Refresh")
                    }

                    Box {

                        IconButton(onClick = { toolbarExpandedStatus = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Localized description"
                            )
                        }

                        DropdownMenu(
                            expanded = toolbarExpandedStatus,
                            onDismissRequest = { toolbarExpandedStatus = false }) {

                            DropdownMenuItem(
                                text = { Text("添加") },
                                onClick = {
                                    toolbarExpandedStatus = false
                                    newTaskDialogStatus = true

                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = null
                                    )
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("设置") },
                                onClick = {
                                    toolbarExpandedStatus = false
                                    downloadNavController.navigate(
                                        DownloadRoutes.DownloadSettingPage.name
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Settings,
                                        contentDescription = null
                                    )
                                }
                            )


                        }
                    }


                }

            )
        },
        floatingActionButton = { TaskFAB(deleteStatus = false) },
        floatingActionButtonPosition = FabPosition.End
    ) {

        Box(
            modifier = Modifier.padding(it)
        ) {

            val downloadingTasks by DownloadViewModel.downloadingTasksFlow.collectAsState()
            val finishedTasks by DownloadViewModel.finishedTasksFlow.collectAsState()

            val pageIndexState: MutableIntState = remember { mutableIntStateOf(DownloadPageTabs.DownloadingTask.index) }
            val pageState = rememberPagerState(pageCount = { DownloadPageTabs.entries.size })

            LaunchedEffect(pageState) {
                snapshotFlow { pageState.currentPage } //TODO figure it out???
                    .collectLatest { newPage ->
                        println("当前页面: $newPage")
                        pageIndexState.intValue = newPage
                    }
            }



            Column {

                DownloadPageTabs(
                    pagerState = pageState,
                    downloadingCount = downloadingTasks.size,
                    completeCount = finishedTasks.size
                )

                HorizontalPager(
                    state = pageState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),

                    ) {

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopStart
                        ) {
                            LazyColumn(
                                verticalArrangement = Arrangement.Top
                            ) {

                                items(
                                    if (pageState.currentPage == DownloadPageTabs.DownloadingTask.index) { downloadingTasks.size }
                                    else { finishedTasks.size }
                                ) {

                                    lateinit var taskList: List<DownloadTask>

                                    if(pageState.currentPage == DownloadPageTabs.DownloadingTask.index){
                                        taskList = downloadingTasks
                                    }

                                    else{
                                        taskList = finishedTasks
                                    }

//                                    Log.d("taskUI","Item ${taskList[it].taskInformation.taskName} update: ${taskList[it].chunkProgress.sum()}/${taskList[it].fileSize} ${(taskList[it].chunkProgress.sum()/taskList[it].fileSize)*100.toFloat()}%")

                                    val progress = taskList[it].chunkProgress.sum()/taskList[it].fileSize.toFloat()


                                    FileTile(
                                        taskID = taskList[it].taskInformation.taskID,
                                        taskName = taskList[it].taskInformation.taskName,
                                        progress = if(progress >= 0F) progress else 1F,
                                        currentSpeed = taskList[it].currentSpeed,
                                        totalSize = taskList[it].fileSize,
                                        onClick = {
//                                            fileTileBottomSheetStatus = true
                                            selectingTile = taskList[it]
                                        },
                                        onLongClick = {}
                                    )



                                }
                            }
                        }


                }


            }
        }
    }

}


@Composable
fun DownloadPageTabs(
    pagerState: PagerState,
    downloadingCount: Int,
    completeCount: Int,
) {

    println("TextTabs rebuild")

    val coroutineScope = rememberCoroutineScope()

    TabRow(
        selectedTabIndex = pagerState.currentPage) {

        Tab(
            text = {
                Text("下载中(${downloadingCount})")
            },
            selected = pagerState.currentPage == 0,
            onClick = {
                //动画也是异步的 需要coroutine使用
                coroutineScope.launch{
                    pagerState.animateScrollToPage(0)
                }

            }
        )

        Tab(
            text = {
                Text("已完成(${completeCount})")
            },
            selected = pagerState.currentPage == 1,
            onClick = {
                coroutineScope.launch{
                    pagerState.animateScrollToPage(1)
                }
            }
        )

    }

}

@Composable
fun TaskFAB(
    deleteStatus:Boolean
) {

    val localContext = LocalContext.current

    var storagePath : Uri? = null
    var fileStoragePath : Uri? = null


    val scope = rememberCoroutineScope()

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

            val targetFile = DocumentsContract.createDocument(
                localContext.contentResolver,
                storagePath!!,
                "",
                "Pili-arm64-v8a-1.0.26.1214.apk"
            )

            val downloadUrl = "https://github.com/guozhigq/pilipala/releases/download/v1.0.26.1214/Pili-arm64-v8a-1.0.26.1214.apk"


            MultiThreadDownloadManager.addTask(
                context = localContext,
                DownloadTask(
                    taskInformation = TaskInformation(
                        taskName = "Pili-arm64-v8a-1.0.26.1214.apk",
                        taskID = downloadUrl.hashCode().toString(), //TODO 相同ID处理机制When..
                        downloadUrl = downloadUrl,
                        storagePath = targetFile!!.toString(),
                    ),

//                    fileSize = 0L,
                    fileSize = 15342905L,
                    speedLimit = 0L,
                    threadCount = 1,
                ),
            )

            scope.launch{
                MultiThreadDownloadManager.waitForAll()
            }


        } ?: println("No directory selected.")
    }

    // 创建启动器来启动文件选择器，并处理结果
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                Log.d("test","uri:$uri")
                fileStoragePath = uri

                fileStoragePath = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FkotlinStartPicture%2FtargetDir%2FJetpack%20Compose.pdf")


            }
        }
    )

    FloatingActionButton(
        onClick = {

//            if(deleteStatus){
//                DownloadViewModel.removeTask("test")
//            }
//
//            else{
//                DownloadViewModel.addTask("test ${DownloadViewModel.downloadingTasksFlow.value.size}","storage/0/Download/test.txt")
//            }




            // 目标写入文件夹 DocumentTree
             directoryLauncher.launch(null)

            // 需要迁移的文件 DocumentUri
//            filePickerLauncher.launch(input = Array<String>(size = 1,init = {index -> "*/*"}))







//            scope.launch{
//                MultiThreadDownloadManager.waitForAll()
//            }


        },
    ) {
        Icon(
            if(deleteStatus) Icons.Default.Delete else Icons.Default.Add,
            contentDescription = "Localized description"
        )
    }
}

fun createEmptyFile(
    context: Context,
    fileName: String,
    storagePath: Uri,
    size: Long,
): Uri? {


    return try {

        val uri = DocumentsContract.createDocument(
            context.contentResolver,
            storagePath, // 之前通过 OpenDocumentTree 获得的目录 Uri
            "application/octet-stream",
            fileName
        )

        // 预分配空间（可选）
        uri?.let {
            preallocateSpace(context,it,size)
        }

        return uri

    }

    catch (e: Exception) {
        println(e.toString())
        return null
    }
}

fun preallocateSpace(context: Context, uri: Uri, targetSize: Long) {
    try {
        context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { fos ->
                // 移动到目标位置-1（因为写入1字节会自动扩展）
                fos.channel.position(targetSize - 1).use {
                    fos.write(0) // 写入单个空字节
                }
            }
        }
    } catch (e: IOException) {
        Log.e("PreAlloc", "空间预分配失败：${e.message}")
    }
}

//suspend fun writeSpace(
//    context: Context,
//    uri: Uri,
//    chunkSize: Int = 2 * 1024 * 1024 // 2MB/chunk
//) {
//
//    val contentResolver = context.contentResolver
////    var currentPosition = 0L
//
//    // 获取文件总大小（示例用固定值）
//    val chunkList = calculateChunks(fixedSize,chunkSize.toLong())
//
//    var bufferLength = 8192
//
//    val scope = CoroutineScope(Dispatchers.IO)
//
//
//    val jobs = mutableListOf<Job>()
//
//    chunkList.map {
//
//        var rangeStart = it.first //start
//
//
//        val job = scope.launch {
//
//            withContext(Dispatchers.IO) {
//                val connection = URL(url).openConnection() as HttpURLConnection
//                    connection.setRequestProperty("Range", "bytes=$start-$end")
//
//                    connection.inputStream.use { input ->
//                }
//
//                contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
//                    FileOutputStream(pfd.fileDescriptor).use { fos ->
//                        fos.channel.use { channel ->
//                            channel.position(rangeStart)
//                            fos.write(bufferLength)
//
//                            println("${Thread.currentThread().name} [$rangeStart/${it.second}]")
//
//                            rangeStart += bufferLength
//
//                            if (it.second - rangeStart < bufferLength) {
//                                bufferLength = (it.second - rangeStart).toInt()
//                            }
//                        }
//                    }
//                }
//
//            }
//
//
//
//            jobs.add(job)
//
//
//        }
//
//
//}
//
//    jobs.joinAll()
//
//}

fun calculateChunks(fileSize: Long, chunkSize: Long): List<Pair<Long, Long>> {
    val chunks = mutableListOf<Pair<Long, Long>>()
    var start = 0L
    while (start < fileSize) {
        val end = (start + chunkSize - 1).coerceAtMost(fileSize - 1)
        chunks.add(start to end)
        start += chunkSize
    }
    return chunks
}
