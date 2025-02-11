package com.example.kotlinstart

import MainViewModel
import RoundedButton
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.kotlinstart.internal.CountBinder
import com.example.kotlinstart.internal.CountBinderService
import com.example.kotlinstart.internal.MyChannel
import com.example.kotlinstart.internal.ProgressBinderService

import com.example.kotlinstart.models.StorageModel
import com.example.kotlinstart.models.StorageModel.getPathFromUri
//import com.example.kotlinstart.ui.theme.KotlinStartTheme

import com.example.kotlinstart.ui.widget.catalogs.MainRoutes
import com.example.kotlinstart.ui.widget.catalogs.downloadPage.DownloadPage
import com.example.kotlinstart.ui.widget.catalogs.imagePage.ImagePage
import com.example.kotlinstart.ui.widget.catalogs.loginPage.LoginPage
import com.example.kotlinstart.ui.widget.catalogs.testPage.TestPage

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        MyChannel.init(context = applicationContext)

        val broadcastIntent = Intent("ACTION_START");

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

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//                ProgressBinderService.progressBinder = service as ProgressBinder

            }
            override fun onServiceDisconnected(name: ComponentName?) {}

        }

        bindService(
            Intent(this, ProgressBinderService::class.java),
            processBinderConnection,
            BIND_AUTO_CREATE
        )



//        val testFile = File("${Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).path}/testFile")
//
//        testFile.createNewFile()

//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
//            addCategory(Intent.CATEGORY_OPENABLE) // 指定只显示图片
//            type = "image/*"
//        }
//
//        startActivityForResult(intent,2)



        setContent {


                val localContext = LocalContext.current

                StorageModel.initModel(LocalContext.current)

                val navController = rememberNavController()

                CompositionLocalProvider(
                    MainViewModel.localNavController provides navController
                ) {
                    NavHost (
                        navController = navController,
                        startDestination = MainRoutes.HomePage.name //initialRoute
                    ) {

                        composable(MainRoutes.HomePage.name) { HomePage() }

                        composable(
                            route = "${MainRoutes.LoginPage.name}/{accountID}",
                            deepLinks = listOf(
                                navDeepLink {
                                    uriPattern = "test://native/{accountID}"
                                    action = Intent.ACTION_VIEW
                                }
                            )
                        ) {

                            var accountID : Long? = null

                            accountID = it.arguments?.getString("accountID")?.toLong()

                            Log.d("Route","accountID:$accountID")
                            LoginPage( accountID = accountID )

                        }

                        composable(MainRoutes.ImagePage.name) { ImagePage() }
                        composable(MainRoutes.DownloadPage.name){ DownloadPage() }
                        composable(MainRoutes.TestPage.name){ TestPage() }


                    }.run {
                        navController.navigate(MainRoutes.DownloadPage.name)
                    }
                }





        }
    }
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
@Preview
fun HomePage(){

    // 获取当前的后退栈条目状态
    val localContext = LocalContext.current
//    val backStackEntry by MainViewModel.localNavController.current.currentBackStackEntryAsState()
//
//    val isRoot = backStackEntry?.destination?.route == MainRoutes.HomePage.name

    BackHandler(true) {
        // 结束当前 Activity
        (localContext as Activity).finish()
    }



    Scaffold(
        floatingActionButton = { Fab() },
        floatingActionButtonPosition = FabPosition.End,
        modifier = Modifier.fillMaxSize()
    ) {
       innerPadding ->

        Box(modifier = Modifier.padding(innerPadding)){
            GreetingPreview()
        }

    }
}


@Composable
fun Fab(
    fabModel: MainViewModel = MainViewModel,
) {

    val textLength by fabModel.mainFabState.collectAsState()

    FloatingActionButton(
        onClick = {
            val currentSymbol:Char = 'a'+textLength.currentValue
            fabModel.increaseCount()

            Log.i("symbol","$currentSymbol")

        },
    ) {
        Text(
            text = "clickHere: ${textLength.currentValue}",
            style = TextStyle(
                color = if (textLength.currentValue > 5) Color.Red else Color.Green
            ),

        )
    }
 }


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
@Preview
fun GreetingPreview() {

    val paddingH12 = @Composable { Spacer(modifier = Modifier.padding(12.dp)) }

    val navController = MainViewModel.localNavController.current

    val localContext = LocalContext.current

    var selectedFileUri: Uri? by remember { mutableStateOf<Uri?>(null) }

    // 创建启动器来启动文件选择器，并处理结果
    val filePickerLauncher = rememberLauncherForActivityResult(
         contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
            if (uri != null) {

                Log.d("test","uri:$uri")

                val currentPath = getPathFromUri(
                    uri = uri,
                    context = localContext
                )

                Log.d("test","uriParse:$currentPath")


            }
        }
    )


    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = {
            dir ->
            if (dir != null) {

                Log.d("test","dir:$dir")

                localContext.contentResolver.takePersistableUriPermission(
                    dir,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                StorageModel.grantedSavePath = dir


            }
        }

    )

    Column {

        val localContext = LocalContext.current

        Greeting("Android")

        RoundedButton(
            name = "打开登录页面",
            onClick = {
                navController.navigate("${MainRoutes.LoginPage.name}/114514")
            }
        )

        paddingH12()

        RoundedButton(
            name = "打开载入图片页面",
            onClick = {
                navController.navigate(MainRoutes.ImagePage.name)
            }
        )

        paddingH12()

        RoundedButton(
            name = "打开测试页面",
            onClick = {
                navController.navigate(MainRoutes.TestPage.name)
            }
        )

        paddingH12()

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

        paddingH12()

        RoundedButton(
            name = "打开目录选择器",
            onClick = {

                val permissionList = localContext.contentResolver.persistedUriPermissions

                if(permissionList.isEmpty()){
                    dirPickerLauncher.launch(
                        input = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    )
                }

                else{
//                    if(permissionList.contains())



//                    if(permissionList.any{ it.uri == StorageModel.grantedSavePath })
//                        if(permissionList.contains{ it.uri == StorageModel.grantedSavePath })
//
//                    for(permission in permissionList){
//                        if(permission.uri == StorageModel.grantedSavePath) break
//                    }

                    StorageModel.grantedSavePath = permissionList.first().uri
                    Log.d("test","StorageModel.grantedSavePath:${StorageModel.grantedSavePath}")
                }


            }
        )

        paddingH12()

        RoundedButton(
            name = "发出测试通知",
            onClick = {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(localContext, android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(
                                localContext as Activity,
                                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                0
                            )

                    }

                    else{
                        println("notification already approved.")
                    }
                }






//                val delay = 5_000L // 5 秒
//                workScheduleNotification(localContext, delay, "Hello", "This is a scheduled notification")
//init
//                ProgressBinderService.progressBinder.startForeground()


//
//                val triggerTime = System.currentTimeMillis() + 10_000
//                scheduleNotification(localContext, triggerTime, "Hello", "This is a scheduled notification")



//                MyChannel.init(context = localContext)

//                val showText = "${System.currentTimeMillis()}"
//
//                val jumpIntent = Intent(
//                    Intent.ACTION_VIEW,
//                    Uri.parse("test://native/${showText}")
//                )
//
//                val pendingIntent = MyChannel.defaultIntent(context = localContext, intent = jumpIntent)
//
//                val testNotificationBuilder = NotificationCompat.Builder(
//                    localContext,
//                    ChannelType.Example.channelName
//                )
//                    .setSmallIcon(R.drawable.ic_launcher_foreground)
//                    .setContentTitle("这是一条通知")
//                    .setContentText(showText)
//                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                    .setContentIntent(pendingIntent)
//                    .setAutoCancel(true)
//                    .build()
//
//                MyChannel.showNotification(
//                    context = localContext,
//                    notificationID = 1,
//                    notification = testNotificationBuilder
//                )


            }
        )

        paddingH12()

        RoundedButton(
            name = "打开下载页面",
            onClick = {
                navController.navigate(MainRoutes.DownloadPage.name)
            }
        )




    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Greeting(name: String,modifier: Modifier = Modifier) {
    Row(

        modifier
            .padding(all = 8.dp),

        ) {

        Box(

            modifier
                .background(color = Color.Red)
                .size(60.dp)
                .padding(end = 12.dp)
                .align(Alignment.CenterVertically)
//                .weight(weight = 3f, fill = true)

        )

        Spacer(modifier = Modifier.size(10.dp))



        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

        ){


            Text(text = "Hello $name!")

            Text(
                text = "write in compose",

                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),

                )

            FlowRow(
                modifier
                    .padding(horizontal = 8.0.dp)
                    .clickable { }
            ) {
                Text(
                    text = "write in compose",

                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),

                    )

                Text(
                    text = "write in compose",

                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),

                    )

                Text(
                    text = "write in compose",

                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),

                    )

                Text(
                    text = "write in compose",

                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),

                    )
            }


        }


    }
}

