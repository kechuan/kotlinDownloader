package com.example.kotlinDownloader.ui.widget.catalogs.downloadPage


import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlinDownloader.internal.*
import com.example.kotlinDownloader.internal.android.DirectoryLauncher
import com.example.kotlinDownloader.models.DownloadViewModel
import com.example.kotlinDownloader.ui.widget.components.GeneralAlertDialog
import kotlinx.coroutines.launch


@SuppressLint("UnrememberedMutableState", "SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingPage(){

    val downloadNavController = DownloadViewModel.localDownloadNavController.current
    val localContext = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var speedLimit by remember { mutableLongStateOf(AppConfig.appConfigs.speedLimit) } // 0 表示不限速
    var threadLimit by remember { mutableIntStateOf(AppConfig.appConfigs.threadLimit) }
    var storagePath by remember { mutableStateOf(AppConfig.appConfigs.storagePath) }

    var resetDialogStatus by remember { mutableStateOf(false) }

    if(resetDialogStatus){
        GeneralAlertDialog(
            title = "重置",
            subTitle = "确认需要还原APP配置吗",
            onDismiss = { resetDialogStatus = false },
            onConfirm = {
                coroutineScope.launch {
                    AppConfig.updateAppConfig(localContext, AppConfigs.Default)
                }.run {
                    speedLimit = AppConfigs.Default.speedLimit
                    threadLimit = AppConfigs.Default.threadLimit
                    storagePath = AppConfigs.Default.storagePath
                }
            }
        )
    }

    Scaffold(
        topBar = {
            FlutterDesignWidget.AppBar(
                onClick = { downloadNavController.popBackStack() },
                title = { Text("DownloadSetting") },
                actions = {

                    IconButton(
                        onClick = {
                            resetDialogStatus = true
                        }
                    ) {
                        Icon(Icons.Default.Refresh, "Trigger restore")
                    }
                }
            )
        },
    ){ paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)){
            Column {

                Box(
                    contentAlignment = Alignment.Center
                ){
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                        ,
                        headlineContent = {
                            Column {

                                Text("存储目录设置", style = TextStyle(fontSize = 18.sp))

                                PaddingV12()

                                Box(
                                    modifier = Modifier
                                        .height(120.dp)
                                        .align(Alignment.CenterHorizontally)
                                    ,

                                ){
                                    Text(
                                        "当前存储目录: $storagePath",
                                        style = TextStyle(fontSize = 16.sp),
                                        maxLines = 3
                                    )
                                }



                            }
                        },

                        trailingContent = {
                            val directoryLauncher = DirectoryLauncher(localContext){

                                coroutineScope.launch {
                                    AppConfig.updateAppConfig(
                                        localContext,
                                        AppConfig.appConfigs.copy(
                                            it.toString()
                                        )
                                    )
                                }


                                storagePath = it.toString()
                            }

                            ElevatedButton(
                                onClick = {
                                    directoryLauncher.launch(null)
                                }
                            ){
                                Text("切换")
                            }

                        }
                    )
                }

                HorizontalDivider()

                Box(
                    contentAlignment = Alignment.Center
                ){
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        headlineContent = {
                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,

                                ){
                                    Text("默认任务限速设置", style = TextStyle(fontSize = 18.sp))
                                    Text(
                                        if(speedLimit == 0L) "不限速" else "${convertBinaryType(speedLimit)}/s",
                                        style = TextStyle(fontSize = 18.sp)
                                    )
                                }

                                PaddingV6()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp)

                                    ,
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically

                                ){

                                    Text("0")

                                    PaddingH6()

                                    Slider(
                                        value = (speedLimit / (20 * BinaryType.MB.size.toFloat())),
                                        onValueChange = {
                                            speedLimit = (it * (20 * BinaryType.MB.size)).toLong()
                                        },
                                        onValueChangeFinished = {

                                            //配置保存

                                            coroutineScope.launch {
                                                AppConfig.updateAppConfig(
                                                    localContext,
                                                    AppConfig.appConfigs.copy(
                                                        speedLimit = speedLimit
                                                    )
                                                )
                                            }




                                        },
                                        valueRange = 0f..1f, // 0-20MB/s
                                    )

                                    PaddingH6()

                                }

                            }
                        },


                    )
                }

                HorizontalDivider()

                Box(
                    contentAlignment = Alignment.Center
                ){
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        headlineContent = {
                            Column(
                                verticalArrangement = Arrangement.Center
                            ){

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    Text("默认任务线程数设置", style = TextStyle(fontSize = 18.sp))
                                    Text("$threadLimit", style = TextStyle(fontSize = 18.sp))
                                }

                                PaddingV6()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp)
                                    ,
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ){

                                    Text("1")

                                    PaddingH6()

                                    Slider(
                                        value = threadLimit / (32).toFloat(),
                                        onValueChange = {
                                            threadLimit = (it * 32).toInt()
                                        },
                                        onValueChangeFinished = {

                                            //配置保存

                                            coroutineScope.launch {
                                                AppConfig.updateAppConfig(
                                                    localContext,
                                                    AppConfig.appConfigs.copy(
                                                        threadLimit = threadLimit
                                                    )
                                                )
                                            }


                                        },
                                        valueRange = 0f..1f, // 0-20MB/s，

                                    )

                                    PaddingH6()
                                }

                            }
                        },


                        )
                }

                HorizontalDivider()


            }
        }

    }




}

