package com.break8t.unknown.ui.encyclopedia

import com.break8t.unknown.ui.components.ScreenTitle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.break8t.unknown.ui.photo.ShareHandler
import com.break8t.unknown.R
import com.break8t.unknown.ui.collection.CollectionKind
import com.break8t.unknown.ui.collection.CollectionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// ponytail: 백엔드가 없어서 불러오는 시간을 흉내 낸다. 저장소가 생기면 그 읽기로 바꾼다.
private const val DEMO_LOAD_MS = 600L

/** 도감 진입점(Figma S-24). 처음에는 불러오는 중(S-24-b)을 보이고, 다 불러온 뒤에만 비어 있음을 판단한다. */
@Composable
fun EncyclopediaScreen(
    onShare: ShareHandler,
    onOpenFieldDrop: () -> Unit,
) {
    var kind by rememberSaveable { mutableStateOf(CollectionKind.CARD) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!loaded) {
            withContext(Dispatchers.Default) { delay(DEMO_LOAD_MS) }
            loaded = true
        }
    }
    CollectionScreen(kind, { kind = it }, loaded, showLocked = true, onShare, onOpenFieldDrop) {
        ScreenTitle(stringResource(R.string.encyclopedia_title))
    }
}
