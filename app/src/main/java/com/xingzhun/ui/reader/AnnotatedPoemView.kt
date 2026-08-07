package com.xingzhun.ui.reader

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingzhun.engine.AnnotatedLine
import com.xingzhun.engine.AnnotatedPoem
import com.xingzhun.engine.CharMeta
import com.xingzhun.engine.ToneClass
import com.xingzhun.ui.theme.Ink
import com.xingzhun.ui.theme.RhymeRed
import com.xingzhun.ui.theme.ToneMark
import kotlinx.coroutines.delay

enum class ReaderLayout { HORIZONTAL, VERTICAL }

/** 平仄记号样式：〇● 或 平仄 */
enum class MarkStyle { SYMBOL, TEXT }

private val PUNCT = setOf('。', '！', '？', '，', '；', '：', '、')

/** 渲染整首诗的平仄记号与韵脚标注 */
@Composable
fun AnnotatedPoemBody(
    annotated: AnnotatedPoem,
    layout: ReaderLayout,
    showTone: Boolean,
    showRhyme: Boolean,
    markStyle: MarkStyle,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    if (layout == ReaderLayout.HORIZONTAL) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            annotated.lines.forEach { line ->
                HorizontalLine(line, showTone, showRhyme, markStyle, fontSize)
            }
        }
    } else {
        // 竖排从右端开始读：首帧后滚动到最右，展示首句及其平仄列
        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState) {
            while (scrollState.maxValue == 0) {
                delay(16)
            }
            scrollState.scrollTo(scrollState.maxValue)
        }
        Row(
            modifier = modifier.horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            annotated.lines.asReversed().forEach { line ->
                VerticalLine(line, showTone, showRhyme, markStyle, fontSize)
            }
        }
    }
}

// ── 横排 ──────────────────────────────────────────────

@Composable
private fun HorizontalLine(
    line: AnnotatedLine,
    showTone: Boolean,
    showRhyme: Boolean,
    markStyle: MarkStyle,
    fontSize: Float,
) {
    val rhyme = if (showRhyme) line.chars.firstOrNull { it.isRhymeWord && it.rhyme != null } else null
    Row(verticalAlignment = Alignment.Top) {
        line.chars.forEach { meta ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showTone && meta.char !in PUNCT) {
                    Text(
                        text = toneMark(meta, markStyle),
                        fontSize = (fontSize * 0.45f).sp,
                        color = ToneMark,
                        lineHeight = (fontSize * 0.62f).sp,
                    )
                } else {
                    Spacer(Modifier.height((fontSize * 0.62f).dp))
                }
                AnnotatedChar(meta, fontSize, showRhyme)
            }
        }
        if (rhyme != null) {
            Text(
                text = "[${rhyme.rhyme}]",
                fontSize = (fontSize * 0.5f).sp,
                color = RhymeRed,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .padding(top = (fontSize * 0.62f).dp),
            )
        }
    }
}

// ── 竖排 ──────────────────────────────────────────────

@Composable
private fun VerticalLine(
    line: AnnotatedLine,
    showTone: Boolean,
    showRhyme: Boolean,
    markStyle: MarkStyle,
    fontSize: Float,
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // 字符列（自上而下）
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            line.chars.forEach { meta ->
                AnnotatedChar(meta, fontSize, showRhyme)
            }
        }
        // 平仄标记列（紧贴字符列右侧）；韵部标签同列展示
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            line.chars.forEach { meta ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showTone && meta.char !in PUNCT) {
                        Text(
                            text = toneMark(meta, markStyle),
                            fontSize = (fontSize * 0.45f).sp,
                            color = ToneMark,
                        )
                    } else {
                        Spacer(Modifier.width((fontSize * 0.45f).dp))
                    }
                    if (showRhyme && meta.isRhymeWord && meta.rhyme != null) {
                        Text(
                            text = "[${meta.rhyme}]",
                            fontSize = (fontSize * 0.4f).sp,
                            color = RhymeRed,
                        )
                    }
                }
            }
        }
    }
}

// ── 公共 ──────────────────────────────────────────────

@Composable
private fun AnnotatedChar(meta: CharMeta, fontSize: Float, showRhyme: Boolean) {
    val rhymed = showRhyme && meta.isRhymeWord && meta.char !in PUNCT
    if (rhymed) {
        Box(
            modifier = Modifier
                .border(1.5.dp, RhymeRed, CircleShape)
                .padding(horizontal = 5.dp),
        ) {
            Text(meta.char.toString(), fontSize = fontSize.sp, color = Ink)
        }
    } else {
        Text(meta.char.toString(), fontSize = fontSize.sp, color = Ink)
    }
}

private fun toneMark(meta: CharMeta, style: MarkStyle): String = when (meta.tone) {
    ToneClass.LEVEL -> if (style == MarkStyle.SYMBOL) "〇" else "平"
    ToneClass.OBLIQUE -> if (style == MarkStyle.SYMBOL) "●" else "仄"
    ToneClass.UNKNOWN -> "？"
}
