package com.example.kotlinDownloader

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.kotlinDownloader.internal.AppConfig
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager
//import com.example.kotlinDownloader.internal.android.CountBinder
//import com.example.kotlinDownloader.internal.android.CountBinderService
import com.example.kotlinDownloader.internal.android.MyChannel
import com.example.kotlinDownloader.internal.android.ProgressBinderService
import com.example.kotlinDownloader.models.DownloadViewModel
import com.example.kotlinDownloader.ui.widget.catalogs.DownloadRoutes
import com.example.kotlinDownloader.ui.widget.catalogs.DownloadSettingPage
import com.example.kotlinDownloader.ui.widget.catalogs.downloadPage.DownloadTaskPage
import com.example.kotlinDownloader.ui.widget.catalogs.testPage.TestPage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val processBinderConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
            override fun onServiceDisconnected(name: ComponentName?) {}
        }

        bindService(
            Intent(this, ProgressBinderService::class.java),
            processBinderConnection,
            BIND_AUTO_CREATE
        )

        val downloadTaskBinderConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
            override fun onServiceDisconnected(name: ComponentName?) {}
        }

        bindService(
            Intent(this, ProgressBinderService::class.java),
            downloadTaskBinderConnection,
            BIND_AUTO_CREATE
        )

        setContent {

            val localContext = LocalContext.current
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()

            scope.launch {
                AppConfig.init(localContext)
                MultiThreadDownloadManager.init(localContext)
                MyChannel.init(context = applicationContext)
            }

            Log.d("config", "${AppConfig.appConfigs}")


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
                    ) {

                        composable(DownloadRoutes.DownloadTaskPage.name) { DownloadTaskPage() }
                        composable(DownloadRoutes.DownloadSettingPage.name) { DownloadSettingPage() }
                        composable(DownloadRoutes.TestPage.name) { TestPage() }

                    }
                }


            }

        }

    }
}


