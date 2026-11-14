package com.break8t.unknown.ui.iso

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.break8t.unknown.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** A one-object scene for list tiles, so a tile shows the same voxel model as the room or island. */
fun thumbnailScene(shape: IsoShape, rotation: Int = 0): IsoScene =
    IsoScene(
        shape.w, shape.d, List(shape.w * shape.d) { Cell(FloorType.NONE) }, listOf(IsoObject("thumb", shape, 0, 0, rotation)),
        floorDepth = 0f, frame = FrameBox(0f, 0f, shape.w.toFloat(), shape.d.toFloat(), 0f, shape.model.sz.toFloat() / VPC),
    )

/**
 * Draws [shape] with the scene rasterizer on the same [PIXEL_DP] grid as the scenes, without smoothing.
 * Renders off the main thread; the tile stays empty for the few milliseconds that takes.
 */
@Composable
fun IsoThumbnail(shape: IsoShape, modifier: Modifier = Modifier, rotation: Int = 0) {
    BoxWithConstraints(modifier) {
        val base = max(1, (PIXEL_DP * LocalDensity.current.density).roundToInt())
        val bw = constraints.maxWidth / base
        val bh = constraints.maxHeight / base
        val scene = remember(shape, rotation) { thumbnailScene(shape, rotation) }
        val cam = IsoCamera.of(ViewPreset.OBLIQUE, 0)
        val inPreview = LocalInspectionMode.current
        val image by produceState<ImageBitmap?>(
            if (inPreview && bw > 0 && bh > 0) toImage(renderScene(scene, cam, bw, bh)) else null, scene, bw, bh,
        ) {
            if (bw > 0 && bh > 0) value = withContext(Dispatchers.Default) { toImage(renderScene(scene, cam, bw, bh)) }
        }
        Canvas(Modifier.size(maxWidth, maxHeight)) {
            val img = image ?: return@Canvas
            val left = ((size.width - img.width * base) / 2f).roundToInt()
            val top = ((size.height - img.height * base) / 2f).roundToInt()
            drawImage(
                img, srcOffset = IntOffset.Zero, srcSize = IntSize(img.width, img.height),
                dstOffset = IntOffset(left, top), dstSize = IntSize(img.width * base, img.height * base),
                filterQuality = FilterQuality.None,
            )
        }
    }
}

private fun toImage(f: IsoFrame): ImageBitmap = Bitmap.createBitmap(f.pixels, f.w, f.h, Bitmap.Config.ARGB_8888).asImageBitmap()

/** Busy indicator in the scene's pixel style: three blocks lit in turn, instead of a smooth spinner. */
@Composable
fun PixelBusy(modifier: Modifier = Modifier, block: Dp = 12.dp) {
    val t by rememberInfiniteTransition(label = "busy").animateFloat(
        0f, 3f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "step",
    )
    Canvas(modifier.size(block * 5, block)) {
        val b = block.toPx()
        for (i in 0 until 3) {
            drawRect(
                if (t.toInt() == i) AppColors.Primary else AppColors.Muted,
                Offset(i * 2 * b, 0f), Size(b, b),
            )
        }
    }
}
