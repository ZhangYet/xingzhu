package com.xingzhu.engine

/** 格律问题类型 */
enum class IssueType {
    /** 孤平：平收句中仅一个平声字（不含韵脚） */
    LONELY_LEVEL,

    /** 三平尾：句末三字皆平 */
    TRIPLE_LEVEL_TAIL,

    /** 三仄尾：句末三字皆仄 */
    TRIPLE_OBLIQUE_TAIL,

    /** 失对：一联内两句关键位平仄未相对 */
    MISMATCH_IN_COUPLET,

    /** 失粘：相邻两联关键位平仄未相粘 */
    MISMATCH_BETWEEN_COUPLETS,

    /** 出韵：韵脚字韵部与首韵脚不一致 */
    RHYME_MISMATCH,
}

/** 一条格律提示 */
data class PingZeIssue(
    val lineIndex: Int,
    val charIndex: Int? = null,
    val type: IssueType,
    val message: String,
)

/**
 * 近体诗格律检测（纯函数）：
 * 体裁识别 + 孤平/三平尾/三仄尾/失对/失粘/出韵 检查。
 * 仅适用于近体诗（五绝/七绝/五律/七律）；待考（UNKNOWN）字跳过对位判断。
 */
object PingZeChecker {

    /** 自动判定近体诗体裁；非近体诗（古体/词/杂言）返回 null */
    fun detectForm(contentText: String): String? {
        val lines = TextSplitter.splitSentences(contentText)
        if (lines.isEmpty()) return null
        val lengths = lines.map { it.count { c -> c in '\u4E00'..'\u9FFF' } }.toSet()
        if (lengths.size != 1) return null
        val len = lengths.first()
        return when {
            len == 5 && lines.size == 4 -> "五言绝句"
            len == 5 && lines.size == 8 -> "五言律诗"
            len == 7 && lines.size == 4 -> "七言绝句"
            len == 7 && lines.size == 8 -> "七言律诗"
            else -> null
        }
    }

    /** 对已标注的近体诗做格律检测，返回问题列表（无问题则空列表） */
    fun check(annotated: AnnotatedPoem, form: String): List<PingZeIssue> {
        val issues = mutableListOf<PingZeIssue>()
        val lines = annotated.lines.map { line ->
            line.chars.filter { it.char in '\u4E00'..'\u9FFF' }
        }
        if (lines.isEmpty()) return issues

        // 句级检查：孤平 / 三平尾 / 三仄尾
        lines.forEachIndexed { idx, chars ->
            if (chars.size < 3) return@forEachIndexed

            val tail = chars.takeLast(3).map { it.tone }
            when {
                tail.all { it == ToneClass.LEVEL } ->
                    issues += PingZeIssue(idx, type = IssueType.TRIPLE_LEVEL_TAIL, message = "第 ${idx + 1} 句三平尾（句末三字皆平）")
                tail.all { it == ToneClass.OBLIQUE } ->
                    issues += PingZeIssue(idx, type = IssueType.TRIPLE_OBLIQUE_TAIL, message = "第 ${idx + 1} 句三仄尾（句末三字皆仄）")
            }

            // 孤平：平收句式（平平仄仄平 / 仄仄平平仄仄平）中"平"被挤成仄，
            // 特征：句末为平、倒数第 2/3 位皆仄、且除韵脚外仅一个平声。
            // （避免误伤合法的"仄仄仄平平"——其倒数第 3 位为平）
            if (chars.last().tone == ToneClass.LEVEL && chars.size >= 3) {
                val levelCount = chars.dropLast(1).count { it.tone == ToneClass.LEVEL }
                val tailOblique = chars[chars.size - 2].tone == ToneClass.OBLIQUE &&
                    chars[chars.size - 3].tone == ToneClass.OBLIQUE
                if (levelCount == 1 && tailOblique) {
                    issues += PingZeIssue(
                        idx,
                        type = IssueType.LONELY_LEVEL,
                        message = "第 ${idx + 1} 句犯孤平（句中仅一个平声字）",
                    )
                }
            }
        }

        val keyPos = keyPositions(lines[0].size)

        // 失对：一联内（0-1, 2-3, ...）两句关键位平仄应相反
        for (i in 0 until lines.size - 1 step 2) {
            if (!isOpposite(lines[i], lines[i + 1], keyPos)) {
                issues += PingZeIssue(
                    i,
                    type = IssueType.MISMATCH_IN_COUPLET,
                    message = "第 ${i / 2 + 1} 联失对（两句关键位平仄未相对）",
                )
            }
        }

        // 失粘：相邻两联（1-2, 3-4, ...）出句与前联对句关键位平仄应相粘
        for (i in 1 until lines.size - 1 step 2) {
            if (!isSame(lines[i], lines[i + 1], keyPos)) {
                issues += PingZeIssue(
                    i,
                    type = IssueType.MISMATCH_BETWEEN_COUPLETS,
                    message = "第 ${i + 1} 句与第 ${i + 2} 句失粘",
                )
            }
        }

        // 出韵：近体诗只在偶句（2、4、6、8 句）押韵，比较这些句的韵脚韵部是否一致
        val rhymeLines = annotated.lines.filterIndexed { idx, _ -> idx % 2 == 1 }
        val rhymes = rhymeLines.mapNotNull { line ->
            line.chars.lastOrNull { it.isRhymeWord && it.rhyme != null }
        }
        val firstRhyme = rhymes.firstOrNull()?.rhyme
        if (firstRhyme != null) {
            rhymes.forEach { meta ->
                if (meta.rhyme != firstRhyme) {
                    val lineIdx = annotated.lines.indexOfFirst { l -> l.chars.any { it === meta } }
                    issues += PingZeIssue(
                        lineIdx,
                        type = IssueType.RHYME_MISMATCH,
                        message = "韵脚『${meta.char}』（${meta.rhyme}）与首韵（$firstRhyme）不同，疑出韵",
                    )
                }
            }
        }

        return issues
    }

    /** 五言取第 2、4 位；七言取第 2、4、6 位（0-based: 1,3 / 1,3,5） */
    private fun keyPositions(lineLen: Int): List<Int> =
        if (lineLen >= 7) listOf(1, 3, 5) else listOf(1, 3)

    private fun isOpposite(a: List<CharMeta>, b: List<CharMeta>, pos: List<Int>): Boolean {
        var checked = 0
        for (p in pos) {
            val ta = a.getOrNull(p)?.tone
            val tb = b.getOrNull(p)?.tone
            if (ta != null && tb != null && ta != ToneClass.UNKNOWN && tb != ToneClass.UNKNOWN) {
                checked++
                if (ta == tb) return false
            }
        }
        return checked > 0
    }

    private fun isSame(a: List<CharMeta>, b: List<CharMeta>, pos: List<Int>): Boolean {
        var checked = 0
        for (p in pos) {
            val ta = a.getOrNull(p)?.tone
            val tb = b.getOrNull(p)?.tone
            if (ta != null && tb != null && ta != ToneClass.UNKNOWN && tb != ToneClass.UNKNOWN) {
                checked++
                if (ta != tb) return false
            }
        }
        return checked > 0
    }
}
