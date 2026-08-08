package com.xingzhu.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingzhu.engine.AnnotatedLine
import com.xingzhu.engine.AnnotatedPoem
import com.xingzhu.engine.CharMeta
import com.xingzhu.engine.ToneClass
import com.xingzhu.ui.theme.Ink
import com.xingzhu.ui.theme.InkSecondary
import com.xingzhu.ui.theme.RhymeRed
import com.xingzhu.ui.theme.RhymeRedBg
import com.xingzhu.ui.theme.ToneLevel
import com.xingzhu.ui.theme.ToneLevelBg
import com.xingzhu.ui.theme.ToneUnknownBg

/** 平仄记号样式：〇● 或 平仄 */
enum class MarkStyle { SYMBOL, TEXT }

private val PUNCT = setOf('。', '！', '？', '，', '；', '：', '、')

/** 渲染整首诗的平仄记号与韵脚标注（横排，纵向滚动） */
@Composable
fun AnnotatedPoemBody(
    annotated: AnnotatedPoem,
    showTone: Boolean,
    showRhyme: Boolean,
    markStyle: MarkStyle,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        annotated.lines.forEach { line ->
            HorizontalLine(line, showTone, showRhyme, markStyle, fontSize)
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
                    ToneMarkBadge(meta.tone, markStyle, fontSize)
                } else {
                    Spacer(Modifier.height(badgeSize(fontSize).dp))
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
                    .padding(top = badgeSize(fontSize).dp),
            )
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

private fun badgeSize(fontSize: Float): Float = fontSize * 0.78f

@Composable
private fun MaterialThemeTypography() = androidx.compose.material3.MaterialTheme.typography.labelMedium

/** 醒目的平仄标记：彩色圆底 + 标记（平=石青描边圆，仄=朱砂实心圆）。
 * 符号用 Canvas 绘制保证与圆底同心；文字模式与待考用 Text。 */
@Composable
private fun ToneMarkBadge(tone: ToneClass, style: MarkStyle, fontSize: Float) {
    val badge = badgeSize(fontSize)
    if (style == MarkStyle.SYMBOL && tone != ToneClass.UNKNOWN) {
        val fg = if (tone == ToneClass.LEVEL) ToneLevel else RhymeRed
        val bg = if (tone == ToneClass.LEVEL) ToneLevelBg else RhymeRedBg
        Box(
            modifier = Modifier
                .size(badge.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(badge.dp * 0.2f),
            ) {
                val stroke = this.size.minDimension * 0.13f
                if (tone == ToneClass.LEVEL) {
                    drawCircle(
                        color = fg,
                        radius = this.size.minDimension / 2 - stroke / 2,
                        style = Stroke(width = stroke),
                    )
                } else {
                    drawCircle(color = fg, style = Fill)
                }
            }
        }
    } else {
        val (fg, bg, text) = when (tone) {
            ToneClass.LEVEL -> Triple(
                ToneLevel,
                ToneLevelBg,
                if (style == MarkStyle.SYMBOL) "〇" else "平",
            )
            ToneClass.OBLIQUE -> Triple(
                RhymeRed,
                RhymeRedBg,
                if (style == MarkStyle.SYMBOL) "●" else "仄",
            )
            ToneClass.UNKNOWN -> Triple(InkSecondary, ToneUnknownBg, "？")
        }
        Box(
            modifier = Modifier
                .size(badge.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = fg,
                fontSize = (fontSize * 0.52f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** 平仄记号图例：展示各标记含义，帮助理解〇●（或平仄）等符号 */
@Composable
fun ToneMarkLegend(
    markStyle: MarkStyle,
    showRhyme: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "平仄标记说明",
            style = MaterialThemeTypography(),
            color = InkSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendItem(ToneClass.LEVEL, "平声", markStyle)
            LegendItem(ToneClass.OBLIQUE, "仄声", markStyle)
            LegendItem(ToneClass.UNKNOWN, "待考", markStyle)
        }
        if (showRhyme) {
            Text(
                text = "韵脚字以朱砂圈注，并标注《诗韵新编》韵部（如［七齐］）",
                style = MaterialThemeTypography(),
                color = InkSecondary,
            )
        }
    }
}

@Composable
private fun LegendItem(tone: ToneClass, label: String, markStyle: MarkStyle) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        ToneMarkBadge(tone, markStyle, fontSize = 16f)
        Text(
            text = label,
            style = MaterialThemeTypography(),
            color = InkSecondary,
        )
    }
}
