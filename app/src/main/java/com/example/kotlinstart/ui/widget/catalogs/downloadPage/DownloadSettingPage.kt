package com.example.kotlinstart.ui.widget.catalogs


import FileTile
import RoundedButton
import SettingTile
import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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

import androidx.compose.material3.*
import androidx.compose.runtime.*


import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState", "SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingPage(){

    val parentNavController = MainViewModel.localNavController.current
    val downloadNavController = DownloadViewModel.localDownloadNavController.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {

            FlutterDesignWidget.AppBar(
                onClick = { downloadNavController.popBackStack() },
                title = { Text("DownloadSettingPage") },
            )
        },
    ) {

        Box(
            modifier = Modifier.padding(it)
        ){

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart
            ){
                LazyColumn(
                    verticalArrangement = Arrangement.Top
                ){

                    item{
                        SettingTile(
                            settingName = "默认存储目录",
                            trailingContent = {Text(text = "test")},
                            onPressed = {},
                            onPressedText = "更改"
                        )
                    }



                }


            }
        }



    }


}

