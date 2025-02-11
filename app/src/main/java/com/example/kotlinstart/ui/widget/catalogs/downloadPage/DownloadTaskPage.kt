package com.example.kotlinstart.ui.widget.catalogs.downloadPage

import AddTaskDialog
import DownloadTask
import FileTile
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
//import com.example.kotlinstart.MultiThreadDownloadManager
import com.example.kotlinstart.ui.widget.catalogs.DownloadRoutes
import com.example.kotlinstart.ui.widget.catalogs.downloadPage.preallocateSpace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.joinAll

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

import kotlin.random.Random

@Composable
fun DownloadTaskPage(){
    val parentNavController = MainViewModel.localNavController.current
    val downloadNavController = DownloadViewModel.localDownloadNavController.current

    val localContext = LocalContext.current

    var newTaskDialogStatus by remember { mutableStateOf(false) }



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

//                            HorizontalDivider()
//
//                            DropdownMenuItem(
//                                text = { Text("") },
//                                onClick = {
//                                    toolbarExpandedStatus = false
//                                    downloadNavController.navigate(
//                                        DownloadRoutes.DownloadSettingPage.name
//                                    )
//                                },
//                                leadingIcon = {
//                                    Icon(
//                                        Icons.Outlined.Settings,
//                                        contentDescription = null
//                                    )
//                                }
//                            )

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

            val pageIndexState: MutableIntState = remember { mutableIntStateOf(0) }
            val pageState = rememberPagerState(pageCount = { 2 })

            LaunchedEffect(pageState) {
                snapshotFlow { pageState.currentPage }
                    .collectLatest { newPage ->
                        println("当前页面: $newPage")
                        pageIndexState.intValue = newPage
                    }
            }

            Column {

                TextTabs(
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

//                        LaunchedEffect(downloadingTasks.size) {
//                            while (true) {
//                                delay(500L)
//                                DownloadViewModel.updateAllTask(List(downloadingTasks.size){ Random.nextFloat() })
//                            }
//                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopStart
                        ) {
                            LazyColumn(
                                verticalArrangement = Arrangement.Top
                            ) {



                                items(
                                    if (pageState.currentPage == 0) {
                                        downloadingTasks.size
                                    } else {
                                        finishedTasks.size
                                    }

                                ) {

    //                                val coroutineScope = rememberCoroutineScope()

                                    //需求记忆
                                    //因为重建会包括这里的区域也重建 那么没办法 只能这样了
                                    //                                val floatFlowState = remember { MutableStateFlow(0.0f) }
                                    //                                val progress by floatFlowState.collectAsState()


    //                                LaunchedEffect(downloadingTasks.size) {
    //                                    while (true) {
    //                                        delay(500L)
    //                                        //UI 统一500ms? 更新 不让回调自己触发更新
    //                                        //还是要让 progressCallback 独立 500ms 更新?
    //
    //                                        //只能后者 因为 MultiThreadDownloadManager 只有 job 没有 downloadTask 的数据保持
    //
    //                                        MultiThreadDownloadManager.jobs.map
    //
    //                                        //那么我要做的就是 获取。。这一时刻的 MultiThreadDownloadManager 所有progress?
    //                                        //那么 lastProgress 呢 而且因为关联了 downloadingTasks.size
    //                                        //假设变更之后 lastProgress数据丢失了怎么办 只能自己获取 DownloadViewModel 提取数据了?
    //                                        DownloadViewModel.updateAllTask(List(downloadingTasks.size){ Random.nextFloat() })
    //
    ////                                            DownloadViewModel.updateTaskProgress(
    ////                                                "test $it",
    ////                                                Random.nextFloat()
    ////                                            )
    //
    //                                    }
    //
    //                                }



                                    FileTile(
                                        taskName = "Item $it",
                                        progress = (downloadingTasks[it].chunkProgress.sum()/downloadingTasks[it].fileSize.toFloat()),
                                        totalSize = downloadingTasks[it].fileSize
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
fun TextTabs(
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

const val fixedSize: Long = 14550954

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
                "foobox_x64.cn.v7.40-1.exe"
            )


            MultiThreadDownloadManager.addTask(
                context = localContext,
                DownloadTask(
                    taskName = "foobox_x64.cn.v7.40-1.exe",
                    taskID = "A001", //TODO 相同ID处理机制When..
//                    downloadUrl = "https://cn.ejie.me/uploads/setup_Clover@3.5.6.exe", //HTTP protocol ban
                    downloadUrl = "https://github.com/dream7180/foobox-cn/releases/download/7.40/foobox_x64.cn.v7.40-1.exe",
                    storagePath = targetFile!!,
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
//
//
//
//                fileStoragePath.lastPathSegment

                scope.launch{
//                    writeSpace(
//                        context = localContext,
//                        uri = uri,
//                    )
                }

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
