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
import com.example.kotlinDownloader.internal.MultiThreadDownloadManager
import com.example.kotlinDownloader.internal.MyDataStore
import com.example.kotlinDownloader.internal.android.CountBinder
import com.example.kotlinDownloader.internal.android.CountBinderService
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

        val countBinderConnection = object : ServiceConnection {

            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                Log.d("service response","onServiceConnected")

                CountBinderService.countBinder = service as CountBinder
                CountBinderService.countBinder.increaseCount()
                Log.d("service response","${CountBinderService.countBinder.getCount()}")
            }

            override fun onServiceDisconnected(name: ComponentName?) {}

        }

        bindService(
            Intent(this,CountBinderService::class.java),
            countBinderConnection,
            BIND_AUTO_CREATE
        )

        val processBinderConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
            override fun onServiceDisconnected(name: ComponentName?) {}
        }

        bindService(
            Intent(this, ProgressBinderService::class.java),
            processBinderConnection,
            BIND_AUTO_CREATE
        )


        setContent {

                val localContext = LocalContext.current
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                scope.launch {
                    MyDataStore.init(localContext)
                    MultiThreadDownloadManager.init(localContext)
                    MyChannel.init(context = applicationContext)
                }

                CompositionLocalProvider(
                    DownloadViewModel.localDownloadNavController provides navController
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = DownloadRoutes.DownloadTaskPage.name
                    ) {

                        composable(DownloadRoutes.DownloadTaskPage.name) { DownloadTaskPage() }
                        composable(DownloadRoutes.DownloadSettingPage.name) { DownloadSettingPage() }
                        composable(DownloadRoutes.TestPage.name){ TestPage() }

                    }
                }



        }

    }

}

