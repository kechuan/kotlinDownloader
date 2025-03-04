package com.example.kotlinDownloader.ui.widget.catalogs


enum class DownloadRoutes {
    DownloadTaskPage,
    DownloadSettingPage,
    TestPage
}

enum class DownloadPageTabs(val index:Int){
    DownloadingTask(0),
    FinishedTask(1)
}