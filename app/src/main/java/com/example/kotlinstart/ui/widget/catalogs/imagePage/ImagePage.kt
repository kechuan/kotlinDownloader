package com.example.kotlinstart.ui.widget.catalogs.imagePage



import RoundedButton
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.example.kotlinstart.models.ImageViewModel
import com.example.kotlinstart.models.ImageViewModel.imageDataState
import com.example.kotlinstart.models.ImageViewModel.loadImageData
import com.example.kotlinstart.models.ImageViewType
import com.example.kotlinstart.ui.widget.catalogs.ImageFullScreenViewPage
import com.example.kotlinstart.ui.widget.catalogs.ImageRoutes
import com.example.kotlinstart.ui.widget.catalogs.ImageStarPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageLoadPage(){

    val imageNavController = ImageViewModel.localImageNavController.current
    val parentNavController = MainViewModel.localNavController.current

    val imageLoadCoroutineScope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            FlutterDesignWidget.AppBar(
                onClick = {
                    parentNavController.popBackStack()
                },
                title = { Text("ImageLoadPage") },
                actions = {

                    var expandedStatus by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = {
                            loadImageData(imageLoadCoroutineScope)
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, "Trigger Refresh")
                    }

                    Box{

                        IconButton(onClick = { expandedStatus = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Localized description")
                        }

                        DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                            DropdownMenuItem(
                                text = { Text("历史记录") },
                                onClick = {
                                    expandedStatus = false
                                    imageNavController.navigate(ImageRoutes.ImageStarPage.name)
                                },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                            )

                            DropdownMenuItem(
                                text = { Text("收藏") },
                                onClick = {

                                    expandedStatus = false

                                    val imageIndex = 0

                                    imageNavController.navigate(
                                        "${ImageRoutes.ImageFullScreenViewPage.name}/${ImageViewType.Star.name}/$imageIndex"
                                    )
                                },
                                leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null) }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("设置") },
                                onClick = {
                                    expandedStatus = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
                            )

                        }
                    }


                }

            )
        },

        ) {
            PullToRefreshBox(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),

                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    Log.d("message response","image Refresh")

                    CoroutineScope(Dispatchers.IO).launch{

                        val imageJob = loadImageData(imageLoadCoroutineScope)
                        imageJob.join()
                        Log.d("message response","parse Done")

                        withContext(Dispatchers.Main){
                            isRefreshing = false
                        }

                    }


                },
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(alignment = Alignment.TopCenter)
                    )
                }
            ){
                LazyColumn(modifier = Modifier.fillMaxSize()){
                    item{ ImageLoaderView() }
                }
            }

        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageLoaderView(){

    val imageLoadCoroutineScope = rememberCoroutineScope()
    val imageData by imageDataState.collectAsState()

    var dialogStatus = remember { mutableStateOf(false) }


    if(dialogStatus.value) {
        ImageViewModel.ImageDialog(
            name = imageData.currentImageData?.first,
            linkUrl = imageData.currentImageData?.second,
            onDismissRequest = { dialogStatus.value = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

//        val imageData by imageDataState.collectAsState()

        Box(
            //别忘了 compose 的 padding 是 "外间距"
            //如果你想要实现以前内间距的效果 只能放在外面 也就是这里的Box
            modifier = Modifier.padding(vertical = 24.dp),
        ){
            RoundedButton(
                name = "获取随机图片",
                onClick = {
                    loadImageData(imageLoadCoroutineScope)
                }
            )
        }

        imageData.currentImageData?.second?.let {
            Log.d("message response", "result: ${imageData.currentImageData?.second}")

            return@Column SuspendImageLoader(
                imageUrl = imageData.currentImageData?.second!!,
                dialogStatus = dialogStatus
            )
        }

        return@Column Text("按下以获取图片")


    }


}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuspendImageLoader(
    imageUrl: String?,
    dialogStatus:MutableState<Boolean>
){

    return Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.combinedClickable(
            enabled = true,
            onLongClick = {
                dialogStatus.value = !dialogStatus.value
            },
            onClick = { }

        )
    ){
        var imageLoadedStatus by remember { mutableStateOf(false) }

        if(!imageLoadedStatus){
            CircularProgressIndicator(
                modifier = Modifier.align(alignment = Alignment.Center)
            )
        }

        //无法使用AnimatedVisibility 会导致它无法加载
        //就跟 if(imageLoadedStatus) 一样
        //除非 新增一个Image 专门用来显示 内存图片 然后专门设置一个 ImageRequest 来获取它
        //那我问你 我要这个AsyncImage来干嘛?

        AsyncImage(
            imageUrl,
            contentDescription = null,
            onLoading = { imageLoadedStatus = false },
            onSuccess = { imageLoadedStatus = true },
        )

    }


}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
@Preview
fun ImagePage(){

        val imageNavController = rememberNavController()

        CompositionLocalProvider(
            ImageViewModel.localImageNavController provides imageNavController
        ) {
            NavHost(
                navController = imageNavController,
                startDestination = ImageRoutes.ImageLoadPage.name
            ){
                composable(ImageRoutes.ImageLoadPage.name) { ImageLoadPage() }

                composable(ImageRoutes.ImageStarPage.name){ ImageStarPage() }

                composable(
                    route = "${ImageRoutes.ImageFullScreenViewPage.name}/{imageViewType}/{initialImageIndex}"
                ){

                    val imageViewType = it.arguments?.getString("imageViewType")?.toString()!!
                    val initialImageIndex = it.arguments?.getString("initialImageIndex")?.toInt()

                    ImageFullScreenViewPage(
                        imageViewType = imageViewType,
                        initialImageIndex = initialImageIndex
                    )
                }


            }
        }




}


