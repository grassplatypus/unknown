package com.playground.jeongche.drive

import android.content.Context

/** 앱이 쓸 기어 상태 소스를 고른다. */
object DriveStateSources {
    fun create(context: Context): DriveStateSource = CarDriveStateSource(context)
}
