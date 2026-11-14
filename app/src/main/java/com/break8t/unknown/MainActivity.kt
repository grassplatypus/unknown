package com.break8t.unknown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.break8t.unknown.data.DemoStore
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.drive.CarUxLimitsSource
import com.break8t.unknown.drive.DriveStateSource
import com.break8t.unknown.drive.DriveStateSources
import com.break8t.unknown.drive.LocalUxLimits
import com.break8t.unknown.drive.UxLimitsSource
import com.break8t.unknown.ui.AppShell
import com.break8t.unknown.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private lateinit var drive: DriveStateSource
    private lateinit var ux: UxLimitsSource
    private val demo = DemoStore.state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drive = DriveStateSources.create(applicationContext).also { it.start() }
        ux = CarUxLimitsSource(applicationContext).also { it.start() }
        setContent {
            val mode by drive.mode.collectAsStateWithLifecycle()
            val limits by ux.limits.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalDemoState provides demo, LocalUxLimits provides limits) {
                AppTheme { AppShell(mode) }
            }
        }
    }

    override fun onDestroy() {
        drive.stop()
        ux.stop()
        super.onDestroy()
    }
}
