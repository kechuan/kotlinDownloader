package com.example.kotlinstart.ui.widget.catalogs

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

import com.example.kotlinstart.models.ImageViewModel
import com.example.kotlinstart.models.ImageViewModel.ImageDialog
import com.example.kotlinstart.models.ImageViewModel.getHistoryImages
import com.example.kotlinstart.models.ImageViewModel.getStaredImages
import com.example.kotlinstart.models.ImageViewType
import java.io.File
import kotlin.math.abs
import kotlin.math.withSign

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ImageFullScreenViewPage(
    imageViewType: String,
    initialImageIndex: Int? = 0
) {

    val localContext = LocalContext.current
    val imageNavController = ImageViewModel.localImageNavController.current
    val localDensity = LocalDensity.current

    lateinit var imageFiles: List<File>

    when(imageViewType){
        ImageViewType.Star.name -> {
            imageFiles =  getStaredImages(localContext)
        }

        ImageViewType.History.name -> {
            imageFiles = getHistoryImages(localContext)
        }
    }

    val pagerState = rememberPagerState(
        pageCount = { imageFiles.size },
        initialPage = initialImageIndex?:0
    )
    val scrollEnabled = remember { mutableStateOf(true) }

    var promptStatus by remember { mutableStateOf(true) }
    val promptOffset by animateDpAsState(
        targetValue = ( if (promptStatus) 0.dp else (-40).dp )
    )
    val promptOpacity by animateFloatAsState(
        targetValue = ( if (promptStatus) 1.0f else 0.0f ),
        label = ""
    )

    var dialogStatus by remember { mutableStateOf(false) }


    Box{

        Scaffold(
            topBar = {
                var staredStatus by remember { mutableStateOf(false) }
                var currentImageFileIndex by remember { mutableIntStateOf(pagerState.currentPage) }

                FlutterDesignWidget.AppBar(
                    onClick = {
                        imageNavController.popBackStack()
                    },
                    title = {
                        Text(" ${pagerState.currentPage + 1}/${imageFiles.size} ")
                    },
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = 0,
                                y = with(localDensity) {
                                    promptOffset.toPx().toInt()
                                }
                            )
                        }
                        .alpha(
                            promptOpacity
                        ),
                    actions = {

                        IconToggleButton(
                            checked = staredStatus,
                            onCheckedChange = {
                                staredStatus = !staredStatus

                                if (staredStatus) {

                                    Log.d("test", "star: ${imageFiles[currentImageFileIndex]}")

                                    //Basic: put(Name,UrlLink => *.1 File )
                                    //Or Storage: put it to custom ExternalFile
                                } else {
                                    Log.d(
                                        "test",
                                        "Unstar: ${imageFiles[currentImageFileIndex]}"
                                    )
                                }


                            }
                        ) {

                            if (staredStatus) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "UnStar"
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Star,
                                    contentDescription = "Star"
                                )
                            }

                        }
                    }

                )
            }

        ) { innerPadding ->
            HorizontalPager(
                modifier = Modifier.padding(innerPadding),
                state = pagerState,
                userScrollEnabled = scrollEnabled.value,
                beyondViewportPageCount = 3
            ) {

                ZoomablePagerImage(
                    modifier = Modifier.fillMaxSize(),
                    scrollEnabled = scrollEnabled,
                    imageFile = imageFiles[it],

                    onClick = { promptStatus = !promptStatus },
                    onLongClick = { dialogStatus = !dialogStatus }
                )
            }

            if(dialogStatus){
                ImageDialog(
                    onDismissRequest = { dialogStatus = false },
                    imageFile = imageFiles[pagerState.currentPage]
                )
            }


        }


    }





}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomablePagerImage(
    modifier: Modifier = Modifier,
    imageFile: File,
    scrollEnabled: MutableState<Boolean>,
    minScale: Float = 1f,
    maxScale: Float = 5f,
    isRotation: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    var targetScale by remember { mutableFloatStateOf(1f) }
    val scale = animateFloatAsState(targetValue = maxOf(minScale, minOf(maxScale, targetScale)))
    var rotationState by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(1f) }
    var offsetY by remember { mutableFloatStateOf(1f) }
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }

    Box(
        modifier = Modifier
            .background(Color.Transparent)
            .combinedClickable(
//                enabled = true,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
                onDoubleClick = {
                    if (targetScale >= 2f) {
                        targetScale = 1f
                        offsetX = 1f
                        offsetY = 1f
                        scrollEnabled.value = true
                    } else targetScale = 3f
                },
            )

            .pointerInput(Unit) {

                awaitEachGesture {

                    awaitFirstDown()

                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        targetScale *= zoom
                        val offset = event.calculatePan()
                        if (targetScale <= 1) {
                            offsetX = 1f
                            offsetY = 1f
                            targetScale = 1f
                            scrollEnabled.value = true
                        } else {
                            offsetX += offset.x
                            offsetY += offset.y
                            if (zoom > 1) {
                                scrollEnabled.value = false
                                rotationState += event.calculateRotation()
                            }
                            val imageWidth = screenWidthPx * scale.value
                            val borderReached = imageWidth - screenWidthPx - 2 * abs(offsetX)
                            scrollEnabled.value = borderReached <= 0
                            if (borderReached < 0) {
                                offsetX = ((imageWidth - screenWidthPx) / 2f).withSign(offsetX)
                                if (offset.x != 0f) offsetY -= offset.y
                            }
                        }
                    } while (event.changes.any { it.pressed })


                }


            }

    ) {
        ImageViewModel.ImageShow(
            imageFile = imageFile,
            modifier = modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    if (isRotation) {
                        rotationZ = rotationState
                    }
                    translationX = offsetX
                    translationY = offsetY
                },


        )

    }
}
