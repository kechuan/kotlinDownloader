package com.example.kotlinDownloader

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.kotlinDownloader.internal.AppConfig
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager
import com.example.kotlinDownloader.internal.android.MyChannel
import com.example.kotlinDownloader.internal.android.ProgressBinderService
import com.example.kotlinDownloader.internal.android.TaskProgressBinderService
import com.example.kotlinDownloader.internal.android.defaultBindService
import com.example.kotlinDownloader.models.DownloadViewModel
import com.example.kotlinDownloader.ui.widget.catalogs.DownloadRoutes
import com.example.kotlinDownloader.ui.widget.catalogs.downloadPage.DownloadSettingPage
import com.example.kotlinDownloader.ui.widget.catalogs.downloadPage.DownloadTaskPage
import com.example.kotlinDownloader.ui.widget.catalogs.testPage.TestPage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    //navController 需求
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        defaultBindService(this, ProgressBinderService::class.java)
        defaultBindService(this, TaskProgressBinderService::class.java)

        setContent {

            val localContext = LocalContext.current

            val scope = rememberCoroutineScope()

            navController = rememberNavController()

            MyChannel.init(localContext)

            scope.launch {
                AppConfig.init(localContext)
                MultiThreadDownloadManager.init(localContext)
            }

            CompositionLocalProvider(
                DownloadViewModel.localDownloadNavController provides navController
            ) {
                NavHost(
                    navController = navController,
                    startDestination = DownloadRoutes.DownloadTaskPage.name
                ) {

                    // downloader://[taskID]
                    // 不仅仅是导航 还会自动打开对应的 bottomSheet
                    composable(
                        route = "${DownloadRoutes.DownloadTaskPage.name}/{taskID}",

                        deepLinks = listOf(
                            navDeepLink {
                                uriPattern = "downloader://{taskID}"
                                action = Intent.ACTION_VIEW
                            }


                        )
                    ){
                        var taskID : String? = it.arguments?.getString("taskID")
                        DownloadTaskPage(taskID)
                    }


                    composable(DownloadRoutes.DownloadTaskPage.name) { DownloadTaskPage() }
                    composable(DownloadRoutes.DownloadSettingPage.name) { DownloadSettingPage() }
                    composable(DownloadRoutes.TestPage.name) { TestPage() }
                }


            }.run{
//                navController.navigate(
//                    DownloadRoutes.TestPage.name
//                )
            }

        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { data ->
                    if (data.scheme == "downloader") {
                        val taskID = data.host
                        navController.navigate("${DownloadRoutes.DownloadTaskPage.name}/$taskID") {
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }
}



