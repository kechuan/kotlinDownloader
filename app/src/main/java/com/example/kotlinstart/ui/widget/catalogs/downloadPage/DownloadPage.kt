package com.example.kotlinstart.ui.widget.catalogs.downloadPage


import android.annotation.SuppressLint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
//import com.example.kotlinstart.downloadsettingpage.DownloadSettingPage
import com.example.kotlinstart.ui.widget.catalogs.DownloadRoutes
import com.example.kotlinstart.ui.widget.catalogs.DownloadSettingPage

@SuppressLint("UnrememberedMutableState", "SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadPage(){

    val downloadNavController = rememberNavController()

    CompositionLocalProvider(
        DownloadViewModel.localDownloadNavController provides downloadNavController
    ) {
        NavHost(
            navController = downloadNavController,
            startDestination = DownloadRoutes.DownloadTaskPage.name
        ) {

            composable(DownloadRoutes.DownloadTaskPage.name) { DownloadTaskPage() }
            composable(DownloadRoutes.DownloadSettingPage.name) { DownloadSettingPage() }

        }
    }



}




