package com.break8t.unknown

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.break8t.unknown.ui.account.AccountFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.break8t.unknown.data.DemoStore
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.drive.CarUxLimitsSource
import com.break8t.unknown.drive.DriveStateSource
import com.break8t.unknown.drive.DriveStateSources
import com.break8t.unknown.drive.LocalUxLimits
import com.break8t.unknown.drive.UxLimitsSource
import com.break8t.unknown.ui.AppShell
import com.break8t.unknown.ui.theme.AppColors
import com.break8t.unknown.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private lateinit var drive: DriveStateSource
    private lateinit var ux: UxLimitsSource
    private val demo = DemoStore.state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        AppColors.night = uiMode == Configuration.UI_MODE_NIGHT_YES
        drive = DriveStateSources.create(applicationContext).also { it.start() }
        ux = CarUxLimitsSource(applicationContext).also { it.start() }
        setContent {
            val mode by drive.mode.collectAsStateWithLifecycle()
            val limits by ux.limits.collectAsStateWithLifecycle()
            // 화면을 나누면 Activity가 다시 만들어지므로 로그인 여부는 저장해 둔다.
            var signedIn by rememberSaveable { mutableStateOf(false) }
            CompositionLocalProvider(LocalDemoState provides demo, LocalUxLimits provides limits) {
                AppTheme {
                    if (signedIn) AppShell(mode, onSignOut = { signedIn = false })
                    else AccountFlow(mode, onSignedIn = { signedIn = true })
                }
            }
        }
    }

    override fun onDestroy() {
        drive.stop()
        ux.stop()
        super.onDestroy()
    }
}
