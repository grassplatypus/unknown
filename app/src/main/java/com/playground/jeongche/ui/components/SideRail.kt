package com.playground.jeongche.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.playground.jeongche.ui.Route
import com.playground.jeongche.ui.theme.AppColors

/** 자리만 잡아 둔 구현이다. components 담당이 Figma v4/Rail 모양으로 바꾼다. 시그니처는 유지한다. */
@Composable
fun SideRail(current: Route, onSelect: (Route) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxHeight().width(140.dp).background(AppColors.Primary)) {
        Route.entries.forEach { r ->
            Text(
                stringResource(r.label),
                color = if (r == current) AppColors.Selected else AppColors.TextOnPrimary,
                modifier = Modifier.clickable { onSelect(r) }.padding(16.dp),
            )
        }
    }
}
