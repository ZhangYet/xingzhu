package com.xingzhun.ui.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingzhun.engine.AnnotatedLine
import com.xingzhun.engine.AnnotatedPoem
import com.xingzhun.engine.CharMeta
import com.xingzhun.engine.ToneClass
import com.xingzhun.ui.theme.Ink
import com.xingzhun.ui.theme.InkSecondary
import com.xingzhun.ui.theme.RhymeRed
import com.xingzhun.ui.theme.RhymeRedBg
import com.xingzhun.ui.theme.ToneLevel
import com.xingzhun.ui.theme.ToneLevelBg
import com.xingzhun.ui.theme.ToneUnknownBg

enum class ReaderLayout { HORIZONTAL, VERTICAL }

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

// ── 竖排分页 ───────────────────────────────────────────

/**
 * 竖排阅读：列自右向左，每列按视口高度容纳字数；
 * 超出宽度时横向分页（HorizontalPager），右→左翻页。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalPager(
    annotated: AnnotatedPoem,
    showTone: Boolean,
    showRhyme: Boolean,
    markStyle: MarkStyle,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val pagePadDp = 20.dp
        val space32Dp = 32.dp
        val fontPx = with(density) { fontSize.sp.toPx() }
        val pagePadPx = with(density) { pagePadDp.toPx() }
        val pageContentWidthPx = with(density) { maxWidth.toPx() } - pagePadPx * 2
        val pageHeightPx = with(density) { maxHeight.toPx() }

        val charStep = fontPx * 2.1f
        val charsPerColumn = ((pageHeightPx - pagePadPx) / charStep).toInt().coerceAtLeast(1)
        val markWidth = fontPx * 0.78f

        val pages = remember(annotated.lines, fontSize, pageContentWidthPx, pageHeightPx, showRhyme) {
            val gapPx = with(density) { 6.dp.toPx() }
            buildVerticalPages(
                lines = annotated.lines,
                charsPerColumn = charsPerColumn,
                // 竖排逐字纵向堆叠：列宽 ≈ 字符列（韵脚块含下方韵部标签）+ 标记列 + 间距
                chunkWidth = { chunk ->
                    val hasRhyme = showRhyme && chunk.chars.any { it.isRhymeWord && it.rhyme != null }
                    val charColW = if (hasRhyme) fontPx * 2.2f else fontPx * 1.3f
                    charColW + gapPx + markWidth
                },
                space32 = with(density) { space32Dp.toPx() },
                pageContentWidth = pageContentWidthPx,
            )
        }
        val pagerState = rememberPagerState { pages.size }

        Column(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                pageSpacing = 12.dp,
            ) { pageIndex ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = pagePadDp),
                    horizontalArrangement = Arrangement.spacedBy(space32Dp, Alignment.End),
                    verticalAlignment = Alignment.Top,
                ) {
                    pages[pageIndex].reversed().forEach { chunk ->
                        VerticalLine(chunk, showTone, showRhyme, markStyle, fontSize)
                    }
                }
            }
            Text(
                text = "第 ${pagerState.currentPage + 1} / ${pages.size} 页",
                style = MaterialThemeTypography(),
                color = InkSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun MaterialThemeTypography() = androidx.compose.material3.MaterialTheme.typography.labelMedium

/** 按字数把行切块，并估计宽度分页 */
private fun buildVerticalPages(
    lines: List<AnnotatedLine>,
    charsPerColumn: Int,
    chunkWidth: (AnnotatedLine) -> Float,
    space32: Float,
    pageContentWidth: Float,
): List<List<AnnotatedLine>> {
    val pages = mutableListOf<List<AnnotatedLine>>()
    var current = mutableListOf<AnnotatedLine>()
    var used = 0f
    for (line in lines) {
        for (chunk in splitChunks(line, charsPerColumn)) {
            val w = chunkWidth(chunk)
            if (current.isNotEmpty() && used + space32 + w > pageContentWidth) {
                pages.add(current)
                current = mutableListOf()
                used = 0f
            }
            current.add(chunk)
            used += w
        }
    }
    if (current.isNotEmpty()) pages.add(current)
    return pages
}

/** 把一行切为若干块，每块至多 charsPerColumn 字；韵脚仅在最后一块 */
private fun splitChunks(line: AnnotatedLine, charsPerColumn: Int): List<AnnotatedLine> {
    if (line.chars.size <= charsPerColumn) return listOf(line)
    val chunks = mutableListOf<AnnotatedLine>()
    var i = 0
    while (i < line.chars.size) {
        val end = (i + charsPerColumn).coerceAtMost(line.chars.size)
        val slice = line.chars.subList(i, end)
        val isLast = end >= line.chars.size
        val meta = slice.mapIndexed { idx, m ->
            if (!isLast && idx == slice.size - 1) m.copy(isRhymeWord = false) else m
        }
        chunks.add(AnnotatedLine(text = slice.joinToString("") { it.char.toString() }, chars = meta))
        i = end
    }
    return chunks
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

// ── 竖排单元 ───────────────────────────────────────────

@Composable
private fun VerticalLine(
    line: AnnotatedLine,
    showTone: Boolean,
    showRhyme: Boolean,
    markStyle: MarkStyle,
    fontSize: Float,
) {
    // 每字一个等高单元（字 + 下方韵部标签行），与分页估算的 charStep 一致：
    // 字与平仄标记逐行对齐、均匀分布；韵脚标签置于字下方，槽位预留字形溢出空间
    val pitch = (fontSize * 2.1f).dp
    val labelSlot = (fontSize * 0.9f).dp
    Row(verticalAlignment = Alignment.Top) {
        // 字符列（自上而下；韵脚标签在字下方）
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            line.chars.forEach { meta ->
                Column(
                    modifier = Modifier.height(pitch),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnnotatedChar(meta, fontSize, showRhyme)
                    }
                    Box(
                        modifier = Modifier.height(labelSlot),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (showRhyme && meta.isRhymeWord && meta.rhyme != null) {
                            Text(
                                text = "[${meta.rhyme}]",
                                fontSize = (fontSize * 0.5f).sp,
                                color = RhymeRed,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        // 平仄标记列：仅标记，与字等高对齐，形成一条直线
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            line.chars.forEach { meta ->
                Column(
                    modifier = Modifier.height(pitch),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (showTone && meta.char !in PUNCT) {
                            ToneMarkBadge(meta.tone, markStyle, fontSize)
                        }
                    }
                    Box(modifier = Modifier.height(labelSlot))
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

private fun badgeSize(fontSize: Float): Float = fontSize * 0.78f

/** 醒目的平仄标记：彩色圆底 + 标记字符（平=石青〇，仄=朱砂●） */
@Composable
private fun ToneMarkBadge(tone: ToneClass, style: MarkStyle, fontSize: Float) {
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
            .size(badgeSize(fontSize).dp)
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
