package com.break8t.unknown.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 픽셀 아트용 누름 표시. 물결 대신 누르는 동안 2dp 내려앉고 [shade]를 덮으며, 떼면 바로 돌아온다.
 * 서서히 변하는 애니메이션은 넣지 않는다(디자인 패턴과 주행 중 정적 화면 규칙).
 */
class PixelPress(private val shade: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = PressNode(interactionSource, shade)

    override fun equals(other: Any?) = other is PixelPress && other.shade == shade
    override fun hashCode() = shade.hashCode()

    private class PressNode(private val source: InteractionSource, private val shade: Color) : Modifier.Node(), DrawModifierNode {
        private var pressed = 0

        override fun onAttach() {
            coroutineScope.launch {
                source.interactions.collect { i ->
                    when (i) {
                        is PressInteraction.Press -> pressed++
                        is PressInteraction.Release, is PressInteraction.Cancel -> pressed = (pressed - 1).coerceAtLeast(0)
                        else -> return@collect
                    }
                    invalidateDraw()
                }
            }
        }

        override fun ContentDrawScope.draw() {
            if (pressed == 0) return drawContent()
            translate(top = SINK.dp.toPx()) {
                this@draw.drawContent()
                drawRect(shade)
            }
        }
    }
}

private const val SINK = 2

/** 앱 전체 기본값. 밝은 바탕을 한 단계 어둡게 한다. */
val PixelIndication = PixelPress(Color.Black.copy(alpha = 0.14f))

/** 갈색처럼 진한 바탕용. 어둡게 하면 거의 티가 나지 않아서 밝게 한다. */
val PixelIndicationOnDark = PixelPress(Color.White.copy(alpha = 0.3f))
