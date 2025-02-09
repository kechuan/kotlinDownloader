package com.example.kotlinstart.models


import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.kotlinstart.internal.HttpRequestClient
import com.example.kotlinstart.internal.ImageApi
import com.example.kotlinstart.models.StorageModel.copyFileToSAFDirectory
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import retrofit2.Retrofit
import java.io.File
import kotlin.collections.get


enum class ImageViewType{
    History(),
    Star()
}

@Parcelize
data class ImageViewData(
    val imageViewType: String,
    val initialImageIndex: Int
) : Parcelable

data class ImageDataState(
    var currentImageData: Pair<String, String>? = null
)

object ImageViewModel : ViewModel() {

    private val _imageDataState = MutableStateFlow(ImageDataState())
    val imageDataState = _imageDataState.asStateFlow()

    val localImageNavController = staticCompositionLocalOf<NavController> {
        error("ImageNavController not provided")
    }

    init{
        initModel()
    }

    fun initModel(){
        Log.d("MainViewModel",imageDataState.value.toString())
    }

    fun updateImageInformation(currentImageData: Pair<String, String>){
        _imageDataState.value = _imageDataState.value.copy(
            currentImageData = currentImageData
        )
    }

    fun loadImageData(scope: CoroutineScope) = scope.launch(Dispatchers.IO){
        val result = suspendImageRequest()  //suspend Function
        println("ImageData result: $result")
        withContext(Dispatchers.Main){
            updateImageInformation(result)
        }
    }

    suspend fun suspendImageRequest(): Pair<String, String> = coroutineScope {

        var title: String? = null
        var imageUrl: String? = null

        val retrofitClient: Retrofit = HttpRequestClient.client
        val imgRequest = retrofitClient.create(ImageApi::class.java)

        val response = async { imgRequest.getDefaultData().execute() } //可读性提升
        val responseData = response.await().body()

        responseData?.let{

            try {

                Log.d("message response","result: ${it["data"]}")

                val dataList = it["data"] as List<*>
                val dataMap = dataList[0] as Map<*, *>
                val urlsData = dataMap["urls"] as Map<*, *>

                title = dataMap["title"] as String?
                imageUrl = urlsData["original"] as String?

//                Log.d("message response","result: $imageUrl")

            }

            catch (jsonError: JsonSyntaxException){
                Log.d("message response","$jsonError")
            }

        }

        return@coroutineScope Pair(title?:"", imageUrl?:"") //嗯? 相当于是 Future<String?> 的概念吧


    }

    fun getHistoryImages(context: Context): List<File>{
        val cacheDir = File(context.cacheDir, "coil3_disk_cache") // 假设Coil缓存路径为 cacheDir/image_cache
        return cacheDir.listFiles()?.filter { it.isFile && it.name.endsWith(".1") } ?: emptyList()
    }

    fun getStaredImages(context: Context): List<File> {
        // 获取Coil默认的缓存目录
        val cacheDir = File(context.cacheDir, "coil3_disk_cache") // 假设Coil缓存路径为 cacheDir/image_cache
        return cacheDir.listFiles()
            ?.filter {
                it.isFile && it.name.endsWith(".1")
            }
            ?.slice(indices = IntRange(0,9))
            ?: emptyList()
    }


    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun ImageShow(
        modifier: Modifier = Modifier,
        imageFile: File,
        contentScale:  ContentScale = ContentScale.Fit
    ){

        AsyncImage(
            imageFile,
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier
                .padding(4.dp)
                    then modifier// 每个图片的内间距
        )

    }


    @Composable
    fun ImageDialog(
        name: String? = null,
        linkUrl: String? = null,
        imageFile: File? = null,

        onDismissRequest: () -> Unit = {}
    ){

            val clipboardManager = LocalClipboardManager.current
            val localContext = LocalContext.current

            Dialog(
                onDismissRequest = onDismissRequest
            ) {

                Surface(
                    shape = RoundedCornerShape(36.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .size(300.dp),

                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {

                            Text(name?:"picture title", fontSize = 24.sp)

                            Log.d("test","${StorageModel.grantedSavePath}/${imageFile?.name}")



                            StorageModel.grantedSavePath?.let{
                                imageFile?.let{
                                    FlutterDesignWidget.ListTile(
                                        "Save",
                                        onClick = {
                                            copyFileToSAFDirectory(
                                                context = localContext,
                                                saveDirectoryUri = StorageModel.grantedSavePath,
                                                sourceFile = imageFile,
                                                targetFileName = "imageFile.jpg"
                                            )
                                            Log.d("message response", "Save")
                                        }
                                    )
                                }

                            }

                            FlutterDesignWidget.ListTile(
                                "Star",
                                onClick = {
                                    Log.d("message response", "Star")
                                }
                            )

                            if(linkUrl!=null){
                                FlutterDesignWidget.ListTile(
                                    "CopyLink",
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(text = linkUrl))
                                    }
                                )
                            }



                        }
                    }



                }
            }




    }


}





