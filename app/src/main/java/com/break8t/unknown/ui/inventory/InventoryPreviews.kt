package com.break8t.unknown.ui.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.break8t.unknown.R
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.shop.PreviewFrame
import com.break8t.unknown.ui.shop.ScreenPreviews
import com.break8t.unknown.ui.shop.ToastHost

// Preview-only states. The default Profile matches the Figma sample (rug x3 selected).
private val discardBody: @Composable ColumnScope.() -> Unit = { DiscardDialogBody("it_rug", "러그", 3, 3, {}, {}, {}) }

@Composable
private fun Inventory(
    tab: InventoryTab = InventoryTab.FURNITURE,
    profile: Profile = Profile(),
    toast: String? = null,
    discard: Boolean = false,
) = PreviewFrame(if (discard) discardBody else null) {
    Box(Modifier.fillMaxSize()) {
        InventoryContent(profile, tab, "it_rug", {}, {}, {}, {}, {}, {})
        ToastHost(toast) {}
    }
}

@ScreenPreviews @Composable private fun S18Furniture() = Inventory()
@ScreenPreviews @Composable private fun S18Seed() = Inventory(InventoryTab.SEED)
@ScreenPreviews @Composable private fun S18Empty() = Inventory(InventoryTab.SEED, Profile(items = emptyMap()))
@ScreenPreviews @Composable private fun D13Discard() = Inventory(discard = true)
@ScreenPreviews @Composable private fun OutdoorBoardPlaced() = Inventory(InventoryTab.OUTDOOR)
@ScreenPreviews @Composable private fun OutdoorBoardStored() = Inventory(InventoryTab.OUTDOOR, Profile(guestbookBoardPlaced = false))

@ScreenPreviews @Composable
private fun T07Discarded() = Inventory(toast = stringResource(R.string.inventory_toast_discarded, "러그", 3))
