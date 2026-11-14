package com.break8t.unknown.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.break8t.unknown.drive.DriveMode
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.theme.AppColors

/** 홈 진입점. 자리만 잡아 둔 구현이다. home 담당이 채우고, 시그니처는 유지한다. */
@Composable
fun HomeScreen(mode: DriveMode, onNavigate: (Route) -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("홈 ($mode)", color = AppColors.TextSub) }
}
