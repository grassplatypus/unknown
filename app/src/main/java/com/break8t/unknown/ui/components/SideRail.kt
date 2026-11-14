package com.break8t.unknown.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.theme.AppColors

// Figma v4/Rail. Sizes are Figma px x 1.3 (app frame 1284x901 px vs window 1682x1210 dp).
private val ItemShape = RoundedCornerShape(26.dp)

/** Figma v4/Rail. 주행 중에도 모양은 같고 고른 항목만 강조한다. */
@Composable
fun SideRail(current: Route, onSelect: (Route) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxHeight()
            .width(182.dp)
            .background(AppColors.Primary)
            .selectableGroup()
            .padding(horizontal = 18.dp, vertical = 35.dp),
        verticalArrangement = Arrangement.spacedBy(35.dp),
    ) {
        Route.entries.forEach { r -> RailItem(r, r == current) { onSelect(r) } }
    }
}

@Composable
private fun RailItem(route: Route, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) AppColors.RailSelected else AppColors.TextOnPrimary
    Column(
        Modifier
            .size(146.dp)
            .clip(ItemShape)
            .background(if (selected) AppColors.Card else Color.Transparent)
            .selectable(selected, onClick = onClick, role = Role.Tab)
            .padding(top = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(iconFor(route)), contentDescription = null, Modifier.size(56.dp), tint = tint)
        Text(
            stringResource(route.label),
            Modifier.padding(top = 10.dp),
            color = tint,
            fontSize = 32.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@DrawableRes
private fun iconFor(route: Route): Int = when (route) {
    Route.HOME -> R.drawable.ic_home
    Route.ISLAND -> R.drawable.ic_farm
    Route.ENCYCLOPEDIA -> R.drawable.ic_book
    Route.FIELD_DROP -> R.drawable.ic_pin
    Route.MY_INFO -> R.drawable.ic_me
    Route.SETTINGS -> R.drawable.ic_set
}
