package com.example.kotlinstart.ui.widget.catalogs.downloadPage

import AddTaskDialog
import FileTile
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
//import com.example.kotlinstart.MultiThreadDownloadManager
import com.example.kotlinstart.ui.widget.catalogs.DownloadRoutes

import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.collectLatest

import kotlinx.coroutines.launch

import kotlin.random.Random

@Composable
fun DownloadTaskPage(){
    val parentNavController = MainViewModel.localNavController.current
    val downloadNavController = DownloadViewModel.localDownloadNavController.current


//    var newTaskDialogStatus = remember { mutableStateOf(false) }
    var newTaskDialogStatus by remember { mutableStateOf(false) }

    if(newTaskDialogStatus) {
        AddTaskDialog(
            linkUrl = "linkHere",
            onDismiss = { newTaskDialogStatus = false },
            onConfirm = { downloadTask ->
                DownloadViewModel.addTask(
                    downloadTask.taskName,
                    downloadTask.storagePath
                )
            },
            defaultSavePath = "..",
            dialogStatus = newTaskDialogStatus

        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {

            FlutterDesignWidget.AppBar(
                onClick = {
//                    downloadNavController.popBackStack( ImageRoutes.ImageLoadPage.name, inclusive = true)
//
//                    downloadNavController.navigate(ImageRoutes.ImageLoadPage.name){
//                        popUpTo(ImageRoutes.ImageLoadPage.name){ inclusive = true }
//                    }
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

                        LaunchedEffect(downloadingTasks.size) {
                            while (true) {
                                delay(500L)
                                DownloadViewModel.updateAllTask(List(downloadingTasks.size){ Random.nextFloat() })
                            }
                        }

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
                                        progress = downloadingTasks[it].progress,
                                        totalSize = 1024*1000
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


@Composable
fun TaskFAB(
    deleteStatus:Boolean
) {

    FloatingActionButton(
        onClick = {
            if(deleteStatus){
                DownloadViewModel.removeTask("test")
            }

            else{
                DownloadViewModel.addTask("test ${DownloadViewModel.downloadingTasksFlow.value.size}","storage/0/Download/test.txt")
            }
        },
    ) {
        Icon(
            if(deleteStatus) Icons.Default.Delete else Icons.Default.Add,
            contentDescription = "Localized description"
        )
    }
}