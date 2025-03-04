package com.example.kotlinDownloader.ui.widget.catalogs


import android.annotation.SuppressLint

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.*
import androidx.compose.runtime.*


import androidx.compose.ui.Modifier
import com.example.kotlinDownloader.downloadsettingpagecontent.DownloadSettingPageContent
import com.example.kotlinDownloader.models.DownloadViewModel


@SuppressLint("UnrememberedMutableState", "SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingPage(){

    val downloadNavController = DownloadViewModel.localDownloadNavController.current

    Scaffold { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)){
            DownloadSettingPageContent()
        }

    }




}

