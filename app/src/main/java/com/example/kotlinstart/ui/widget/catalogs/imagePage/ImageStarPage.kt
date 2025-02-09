package com.example.kotlinstart.ui.widget.catalogs

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.kotlinstart.models.ImageViewModel
import com.example.kotlinstart.models.ImageViewType
import java.io.File


@Composable
fun ImageStarPage() {

    val imageNavController = ImageViewModel.localImageNavController.current

    Scaffold(

        topBar = {
            FlutterDesignWidget.AppBar(
                title = { Text("Star Page") },
                onClick = {
                    imageNavController.popBackStack()
                },
                actions = {
                    IconButton(
                        onClick = {

                        }
                    ) {
                        Icon(Icons.Filled.Close, "Remove Mode")
                    }
                }
            )
        },



    ) {
        innerPadding -> StarImageGridView(innerPadding)
    }


}

//fun NavController.openNewsDetails(article: ImageList,name:String) {
//    currentBackStackEntry?.savedStateHandle?.apply{
//
//        set(
//            "article",
//            article
//        )
//
//        set(
//            "name",
//            name
//        )
//    }
//
//    navigate(ScreenConst.NEWS_DETAILS)
//}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun StarImageGridView(innerPadding: PaddingValues){

    val localContext = LocalContext.current
    val cachedImages: List<File> = ImageViewModel.getHistoryImages(localContext)
    val imageNavController = ImageViewModel.localImageNavController.current

//    var dialogStatus by remember { mutableStateOf(false) }
//    var surfingImageIndex by remember { mutableIntStateOf(0) }

    BoxWithConstraints{
        Log.d("test","outer:${maxWidth}/${maxHeight}")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3), // 显示3列
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

                items(cachedImages.size) {

                        imageIndex ->

                    Box(
                        modifier = Modifier.clickable(
                            onClick = {

                                Log.d("test", "gridView trigger")

                                imageNavController.navigate(
                                    "${ImageRoutes.ImageFullScreenViewPage.name}/${ImageViewType.History.name}/${imageIndex}"
                                )

                            },
                        )
                    ){

                        ImageViewModel.ImageShow(
                            imageFile = cachedImages[imageIndex],

                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                        )

                    }
            }
        }


    }


}





