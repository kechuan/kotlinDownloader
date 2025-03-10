package com.example.kotlinDownloader.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.kotlinDownloader.internal.DownloadTask
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager
import kotlinx.coroutines.flow.StateFlow


object DownloadViewModel: ViewModel() {

    val localDownloadNavController = staticCompositionLocalOf<NavController> {
        error("downloadNavController not provided")
    }

    val downloadingTasksFlow: StateFlow<List<DownloadTask>> = MultiThreadDownloadManager.downloadingTaskFlow
    val finishedTasksFlow: StateFlow<List<DownloadTask>> = MultiThreadDownloadManager.finishedTaskFlow

    val MultiChooseTaskIDSet = mutableSetOf<String>()



}