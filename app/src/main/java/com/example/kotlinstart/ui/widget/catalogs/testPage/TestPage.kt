package com.example.kotlinstart.ui.widget.catalogs.testPage

import RoundedButton
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.kotlinstart.Greeting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
fun TestPage() {
//    PullToRefreshWithLoadingIndicatorSample()
//    TransformGestureDemo()
    UriReadImage()
}

@Composable
fun CoTestPage(){
    val inputText = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        LaunchedEffect(
            key1 = "test"
        ){
            scope.launch(Dispatchers.Main) {
                for (i in 0..10) {
                    delay(500)
                    Log.d("test","$i")
                }
            }
        }


        DisposableEffect(key1 = inputText.value) {
            Log.d("test","boot")
            onDispose {
                Log.d("test","on Dispose")
            }
        }

        Text(
            text = "Hello",
            modifier = Modifier.padding(bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = inputText.value,
            onValueChange = { inputText.value = it },
            label = { Text(text = "Name") },
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PullToRefreshWithLoadingIndicatorSample() {
//    var itemCount by remember { mutableIntStateOf(0) }

    val test = LocalContext.current

    val testCases = listOf(
        List(4) { "Item $it" } + listOf("Item 8", "Item 9"),
        List(12) { "Item $it" },
        List(12) { "Item $it" }.shuffled(),
        List(20) { "Item $it" },
        listOf("Item 1")
    )


    var itemsList by remember { mutableStateOf( List(10){ "Item $it" }  )}

    var rememberCount by remember { mutableIntStateOf(0) }


    var isRefreshing by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            delay(2000)
            itemsList = testCases[rememberCount % 5]
            rememberCount++
//            itemCount += 5
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Title") },
                // Provide an accessible alternative to trigger refresh.
                actions = {
                    IconButton(
                        onClick = onRefresh
                    ) {
                        Icon(Icons.Filled.Refresh, "Trigger Refresh")
                    }
                }
            )
        }
    ) {



        PullToRefreshBox(
            modifier =
                Modifier.padding(it),

            state = state,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = state,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            LazyColumn(
                Modifier.fillMaxSize()
            ) {
                items(itemsList.size) {



//                        ListItem(
//                            headlineContent = { Text(text = itemsList.get(it)) },
//                            modifier = Modifier
//                                .offset(
//                                    y = animateFloatAsState(
//                                        targetValue = if ( itemsList.get(it) == "Item ${it}") 0f else 40f,
//                                        animationSpec = tween(durationMillis = 500)
//                                    ).value.dp
//                                )
//
//                                .alpha(if ( itemsList.get(it) == "Item ${it}") 1f else 0f,)
//
//                                .animateItem(
//                                    fadeInSpec = tween(500),
//                                    fadeOutSpec = tween(500),
//                                )
//                        )

                    AnimatedVisibility(
                        visible = itemsList[it] == "Item $it",
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ){
                        ListItem(
                            modifier = Modifier.animateContentSize(
                                animationSpec = tween(2000)
                            ),
                            headlineContent = {
                                Text(text = itemsList[it])
                            }
                        )
                    }

                }
            }

            BoxWithConstraints{

//                val listProcess by animateDpAsState(
//                    targetValue = (
//                            ((itemsList.size)/20f)*maxWidth.value.toInt()).dp
//                )

                val listProcess = ((itemsList.size)/20f)*maxWidth.value.toInt()
                
//                val transition = updateTransition(listProcess)

                val width by animateDpAsState(
                    targetValue = when("true"){
                        "true" -> 200.dp
                        else -> 100.dp
                    }
                )

                Surface(
                    color = Color.Blue
                ) {
//                    Log.d("message response","${listProcess}")

                    Box(
                        modifier =
                        Modifier
                            .animateContentSize()
                            .size(width = listProcess.dp, height = 6.dp)
                            .offset()
                    )
                }
            }

        }

    }


}

@Composable
fun UriReadImage(){

//    val localContext = LocalContext.current

//    var uriLoadedStatus by remember { mutableStateOf(false) }

    var uriInformation by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {

                Log.d("test","uri:$uri")

                uriInformation = uri


            }
        }
    )

    Scaffold {
        innerPadding ->

        Column(
            modifier = Modifier.padding(innerPadding)
        ){

            RoundedButton(
                name = "打开文件选择器",
                onClick = {
                    filePickerLauncher.launch(
                        input = Array<String>(
                            size = 1,
                            init = {index -> "*/*"}
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.padding(12.dp))

            if(uriInformation!=null){
                AsyncImage(
                    uriInformation,
                    contentDescription = null,
                    modifier = Modifier.align(
                        alignment = Alignment.CenterHorizontally
                    )
                )
            }
        }


    }




}

@Composable
fun TransformGestureDemo() {
    var boxSize = 100.dp
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(boxSize)
                .rotate(rotationAngle)
                // 需要注意offset与rotate的调用先后顺序
                .scale(scale)
                .offset {
                    IntOffset(
                        offset.x.roundToInt(), offset.y.roundToInt()
                    )
                }
                .background(Color.Green)
                .pointerInput(Unit) {
                    detectTransformGestures(
                        panZoomLock = true, // 平移或放大时是否可以旋转
                        onGesture = {
                                _, pan: Offset,zoom: Float,rotation: Float ->
                            offset += pan
                            scale *= zoom
                            rotationAngle += rotation
                        }
                    )

                }
        )
    }
}