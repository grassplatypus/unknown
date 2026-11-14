package com.break8t.unknown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.break8t.unknown.drive.DriveStateSource
import com.break8t.unknown.drive.DriveStateSources
import com.break8t.unknown.ui.AppShell
import com.break8t.unknown.ui.theme.AppTheme

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
