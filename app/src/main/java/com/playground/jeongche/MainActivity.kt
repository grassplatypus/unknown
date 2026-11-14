package com.playground.jeongche

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playground.jeongche.drive.DriveStateSource
import com.playground.jeongche.drive.DriveStateSources
import com.playground.jeongche.ui.AppShell
import com.playground.jeongche.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private lateinit var drive: DriveStateSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drive = DriveStateSources.create(applicationContext).also { it.start() }
        setContent {
            val mode by drive.mode.collectAsStateWithLifecycle()
            AppTheme { AppShell(mode) }
        }
    }

    override fun onDestroy() {
        drive.stop()
        super.onDestroy()
    }
}
