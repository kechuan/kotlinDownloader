package com.example.kotlinDownloader.internal.android

import android.content.Context
import android.widget.Toast

fun defaultToaster(
    context: Context,
    message: String
){
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}