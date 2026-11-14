package com.playground.jeongche.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playground.jeongche.R
import com.playground.jeongche.ui.Route
import com.playground.jeongche.ui.theme.AppColors
import com.playground.jeongche.ui.theme.AppTheme

// Sample text below is preview-only and never shipped to users.
@Preview(name = "기본 창", widthDp = 1682, heightDp = 1210)
@Preview(name = "화면 나눔", widthDp = 841, heightDp = 1210)
@Composable
private fun ComponentsGallery() = AppTheme {
    Row(Modifier.fillMaxSize().background(AppColors.Background)) {
        SideRail(current = Route.ISLAND, onSelect = {})
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                PrimaryButton("섬으로 들어가기", onClick = {})
                SecondaryButton("취소", onClick = {})
                StateButton(on = true, onChange = {})
                StateButton(on = false, onChange = {})
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                AppChip("엽서 3", selected = true, onClick = {})
                AppChip("캐릭터 4", selected = false, onClick = {})
                AppChip("상점", selected = false, onClick = {})
            }
            AppCard {
                Column(Modifier.padding(24.dp)) {
                    Text("항아리 보쌈", fontSize = 44.sp)
                    Text("1.2km · 4분", color = AppColors.TextSub, fontSize = 30.sp)
                }
            }
            DialogPanel("모몽가를 구매할까요?", "코인 200개를 써요.", "구매", onConfirm = {}, onDismiss = {})
            AppToast("상태 메시지를 바꿨어요")
            Box(Modifier.fillMaxWidth().height(480.dp)) {
                Box(Modifier.fillMaxSize().background(AppColors.Sky).drivingBlur(true), Alignment.Center) {
                    Text("본문", fontSize = 60.sp)
                }
                DrivingCover(stringResource(R.string.park_hint_island))
            }
        }
    }
}
