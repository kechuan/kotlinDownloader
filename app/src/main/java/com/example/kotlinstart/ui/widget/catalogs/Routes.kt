package com.example.kotlinstart.ui.widget.catalogs


enum class MainRoutes {
    HomePage,
    LoginPage,
    ImagePage,
    TestPage,
    DownloadPage
}

enum class ImageRoutes{
    ImageLoadPage,
    ImageStarPage,
    ImageFullScreenViewPage,
}

enum class DownloadRoutes {
    DownloadTaskPage,
    DownloadSettingPage
}

enum class DownloadPageTabs(val index:Int){
    DownloadingTask(0),
    FinishedTask(1)
}