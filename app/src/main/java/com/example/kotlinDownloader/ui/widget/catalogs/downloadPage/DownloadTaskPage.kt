package com.example.kotlinDownloader.ui.widget.catalogs.downloadPage

import com.example.kotlinDownloader.ui.widget.components.AddTaskDialog
import com.example.kotlinDownloader.internal.android.DirectoryLauncher

import FileTile
import FileTileBottomSheet
import RenameTaskDialog
import android.app.Activity
import com.example.kotlinDownloader.internal.DownloadTask
import com.example.kotlinDownloader.internal.TaskInformation
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SensorDoor
import androidx.compose.material.icons.outlined.Settings

import androidx.compose.material3.*
import androidx.compose.runtime.*


import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager
import com.example.kotlinDownloader.internal.MyDataStore
import com.example.kotlinDownloader.internal.android.FilePickerLauncher
import com.example.kotlinDownloader.internal.convertDocUri
import com.example.kotlinDownloader.models.DownloadViewModel
import com.example.kotlinDownloader.ui.widget.catalogs.DownloadPageTabs
import com.example.kotlinDownloader.ui.widget.catalogs.DownloadRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay


import kotlinx.coroutines.flow.collectLatest

import kotlinx.coroutines.launch


@Composable
fun DownloadTaskPage(){
    val downloadNavController = DownloadViewModel.localDownloadNavController.current

    val localContext = LocalContext.current
    val localClipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()


    var multiChooseMode by remember { mutableStateOf(false) }
    var newTaskDialogStatus by remember { mutableStateOf(false) }
    var renameDialogStatus by remember { mutableStateOf(false) }
    var selectingTile by remember { mutableStateOf(DownloadTask.Default) }
    val taskBottomSheetStatus = selectingTile.taskInformation.taskID != DownloadTask.Default.taskInformation.taskID

    var quitReady by remember { mutableStateOf(false) }
    BackHandler(quitReady) { (localContext as Activity).finish() }

    //弹出式窗口区域
    if(newTaskDialogStatus) {
        AddTaskDialog(
            linkUrl = localClipboard.getText()?.text ?: "", //预计会读取剪切板
            onDismiss = { newTaskDialogStatus = false },
            onConfirm = { downloadTask,threadCount ->
                MultiThreadDownloadManager.addTask(
                    context = localContext,
                    downloadTask = downloadTask,
                    threadCount = threadCount
                )
            },
            defaultStoragePath = MyDataStore.storagePath,
        )
    }

    if(renameDialogStatus) {
        RenameTaskDialog(
            taskID = selectingTile.taskInformation.taskID,
            onDismiss = { renameDialogStatus = false },
            onConfirm = { taskID,fileName ->

                // 首先要修改 UI 然后还要修改 数据库?
                MultiThreadDownloadManager.updatefileName(taskID,fileName).also{
                    coroutineScope.launch {
                        MyDataStore.updateTask(
                            context = localContext,
                            taskID = taskID,
                            newTaskInformation = it

                        )
                    }

                }
                renameDialogStatus = false
            },

        )
    }

    if(taskBottomSheetStatus){
        FileTileBottomSheet(
            selectingTask = selectingTile,
            onDismiss = {
                selectingTile = DownloadTask.Default
                renameDialogStatus = false
            },
            renameAction = { renameDialogStatus = true },

        )
    }

    //主页面
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {

            FlutterDesignWidget.AppBar(
                onClick = {
                    //TODO Toaster
                    if(quitReady) (localContext as Activity).finish()
                    quitReady = true

                },
                title = { Text("DownloadPage") },
                actions = {

                    var toolbarExpandedStatus by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { multiChooseMode = !multiChooseMode }
                    ) {
                        Icon(Icons.Filled.DoneAll, "Trigger MultiChoose")
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

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("清除任务") },
                                onClick = {
                                    toolbarExpandedStatus = false
                                    MultiThreadDownloadManager.removeAllDownloadingTasks(localContext)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null
                                    )
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("测试页面") },
                                onClick = {
                                    toolbarExpandedStatus = false
                                    downloadNavController.navigate(DownloadRoutes.TestPage.name)
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.SensorDoor,
                                        contentDescription = null
                                    )
                                }
                            )


                        }
                    }


                }

            )
        },
        floatingActionButton = {
            TaskFAB(
                deleteStatus = multiChooseMode,
                deleteAction = { multiChooseMode = false }
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) {

        Box(
            modifier = Modifier.padding(it)
        ) {

            //相当于 PageController的。。函数切换
            val pageIndexState: MutableIntState = remember { mutableIntStateOf(DownloadPageTabs.DownloadingTask.index) }

            //而这个是 PageController的State 用于暴露给UI的 调整 pageIndexState 方式
            val pageState = rememberPagerState(pageCount = { DownloadPageTabs.entries.size })

            LaunchedEffect(pageState) {
                snapshotFlow { pageState.currentPage } //TODO figure it out???
                    .collectLatest { newPage ->
                        println("当前页面: $newPage")
                        pageIndexState.intValue = newPage
                    }
            }

            val downloadingTasks by DownloadViewModel.downloadingTasksFlow.collectAsState()
            val finishedTasks by DownloadViewModel.finishedTasksFlow.collectAsState()

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

                                var taskList: List<DownloadTask> =
                                    if(pageState.currentPage == DownloadPageTabs.DownloadingTask.index){
                                        downloadingTasks
                                    }

                                    else{
                                        finishedTasks
                                    }


                                items(
                                    count = taskList.size,
                                    key = { taskList[it].taskInformation.taskID }

                                ) {

                                    var totalByteDownloaded = 0L
                                    val chunksRangeList = taskList[it].taskInformation.chunksRangeList

                                    val progress = if(taskList[it].taskInformation.fileSize == 0L) 0F else{

                                        taskList[it].chunkProgress.mapIndexed { chunkIndex,currentChunk ->
                                            if(currentChunk == -1){
                                                totalByteDownloaded += (chunksRangeList[chunkIndex].second - chunksRangeList[chunkIndex].first)+1
                                            }

                                            else{
                                                totalByteDownloaded += currentChunk
                                            }
                                        }

                                        totalByteDownloaded/taskList[it].taskInformation.fileSize.toFloat()

                                    }

                                    Log.d("taskUI","Item ${taskList[it].taskInformation.fileName} update: ${totalByteDownloaded}/${taskList[it].taskInformation.fileSize} ${progress * 100}%")

                                    FileTile(
                                        taskID = taskList[it].taskInformation.taskID,
                                        fileName = taskList[it].taskInformation.fileName,
                                        totalSize = taskList[it].taskInformation.fileSize,
                                        progress = if(progress >= 0F) progress else 1F,
                                        currentSpeed = taskList[it].currentSpeed,
                                        multiChooseMode = multiChooseMode,
                                        onClick = {
                                            selectingTile = taskList[it]
                                        },
                                        onLongClick = {
                                            multiChooseMode = !multiChooseMode
                                            DownloadViewModel.MultiChooseSet.clear()
                                        }
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
    deleteStatus:Boolean,
    deleteAction : () -> Unit = {},
) {

    val localContext = LocalContext.current
    val scope = rememberCoroutineScope()

    val directoryLauncher = DirectoryLauncher(
        context = localContext
    ){ selectedUri ->

        var storagePath : Uri? = DocumentFile.fromTreeUri(localContext, selectedUri)?.uri

        val downloadUrl = "https://github.com/kechuan/banguLite/releases/download/0.5.6%2B1/banguLite-0.5.6.1-armeabi-v7a.apk"
        val name = "banguLite-0.5.6.1-armeabi-v7a.apk"

        val targetFileUri = convertDocUri(
            context = localContext,
            storagePath = storagePath,
            fileName = name
        )

        MultiThreadDownloadManager.addTask(
            context = localContext,
            DownloadTask(
                taskInformation = TaskInformation(
                    fileName = name,
                    taskID = downloadUrl.hashCode().toString(), //TODO 相同ID处理机制When..
                    downloadUrl = downloadUrl,
                    storagePath = targetFileUri!!.toString(),
                ),
            ),
        )

        scope.launch{
            Log.d("taskInfo","try to add task: fileName: $name, taskID:${downloadUrl.hashCode().toString()}")
            MultiThreadDownloadManager.waitForAll()
        }
    }


    // 创建启动器来启动文件选择器，并处理结果 一般是读取
    val filePickerLauncher = FilePickerLauncher{ uri ->
        uri?.let{
            var fileStoragePath : Uri = uri
            Log.d("test","uri:$fileStoragePath")

        }
    }

    FloatingActionButton(
        onClick = {
            if(deleteStatus){
                MultiThreadDownloadManager.removeTasks(localContext)
                deleteAction()
            }

            else{
                directoryLauncher.launch(null)
            }

        },
    ) {
        Icon(
            if(deleteStatus) Icons.Default.Delete else Icons.Default.Add,
            contentDescription = "Localized description"
        )
    }
}


