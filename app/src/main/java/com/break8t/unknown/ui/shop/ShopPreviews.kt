package com.break8t.unknown.ui.shop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.data.Profile

// Preview-only states. The default Profile matches the Figma sample (240 coins).
@Composable
private fun Shop(
    tab: ShopTab,
    profile: Profile = Profile(),
    toast: String? = null,
    dialog: (@Composable ColumnScope.() -> Unit)? = null,
) = PreviewFrame(dialog) {
    Box(Modifier.fillMaxSize()) {
        ShopContent(profile, tab, {}, {}, {}, {})
        ToastHost(toast) {}
    }
}

private fun buyBody(id: String, qty: Int = 1, coins: Int = Profile().coins): @Composable ColumnScope.() -> Unit = {
    BuyDialogBody(Catalog.item(id)!!, coins, qty, BalanceConfig.DEFAULT, {}, {}, {})
}

@ScreenPreviews @Composable private fun S19aFurniture() = Shop(ShopTab.FURNITURE)
@ScreenPreviews @Composable private fun S19bSeed() = Shop(ShopTab.SEED)
@ScreenPreviews @Composable private fun S19eCharacter() = Shop(ShopTab.CHARACTER)
@ScreenPreviews @Composable private fun S19ePose() = Shop(ShopTab.POSE)
@ScreenPreviews @Composable
private fun S19eFireflyOwned() = Shop(ShopTab.CHARACTER, Profile(owned = Profile().owned + "ch_firefly"))
@ScreenPreviews @Composable
private fun S19FromVoicePacks() = PreviewFrame {
    ShopContent(Profile(), ShopTab.CHARACTER, {}, {}, {}, {}, backLabel = R.string.shop_back_voice)
}
@ScreenPreviews @Composable private fun D14aBuy() = Shop(ShopTab.FURNITURE, dialog = buyBody("it_rug", qty = 2))
@ScreenPreviews @Composable private fun D14bShort() = Shop(ShopTab.FURNITURE, dialog = buyBody("it_rug", qty = 4))
@ScreenPreviews @Composable private fun D17aCharacter() = Shop(ShopTab.CHARACTER, dialog = buyBody("ch_firefly"))
@ScreenPreviews @Composable private fun D17bShort() = Shop(ShopTab.CHARACTER, dialog = buyBody("ch_firefly", coins = 150))
@ScreenPreviews @Composable private fun D19Pose() = Shop(ShopTab.POSE, dialog = buyBody("pose_jump"))

@ScreenPreviews @Composable
private fun D18SwitchCharacter() = Shop(ShopTab.CHARACTER, dialog = { SwitchDialogBody(Catalog.item("ch_firefly")!!, {}, {}) })

@ScreenPreviews @Composable
private fun T08Bought() = Shop(ShopTab.FURNITURE, toast = stringResource(R.string.shop_toast_bought, "러그", 2))

@ScreenPreviews @Composable
private fun T09Pose() = Shop(ShopTab.POSE, toast = stringResource(R.string.shop_toast_pose, "점프"))
