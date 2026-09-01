package com.xingzhu.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PingZeCheckerTest {

    private val engine = PingZeEngine()

    private fun checkTones(content: String): List<PingZeIssue> {
        val form = PingZeChecker.detectForm(content)
        assertNotNull("应为近体诗", form)
        val ann = engine.annotatePoem(0L, checkNotNull(form), content)
        return PingZeChecker.check(ann, checkNotNull(form))
    }

    // ── 体裁识别 ─────────────────────────────────────

    @Test
    fun `detect form for regulated verse`() {
        assertEquals("五言绝句", PingZeChecker.detectForm("床前明月光，疑是地上霜。举头望明月，低头思故乡。"))
        assertEquals("五言律诗", PingZeChecker.detectForm(
            "国破山河在，城春草木深。感时花溅泪，恨别鸟惊心。烽火连三月，家书抵万金。白头搔更短，浑欲不胜簪。"))
        assertEquals("七言绝句", PingZeChecker.detectForm("朝辞白帝彩云间，千里江陵一日还。两岸猿声啼不住，轻舟已过万重山。"))
        assertEquals("七言律诗", PingZeChecker.detectForm(
            "风急天高猿啸哀，渚清沙白鸟飞回。无边落木萧萧下，不尽长江滚滚来。万里悲秋常作客，百年多病独登台。艰难苦恨繁霜鬓，潦倒新停浊酒杯。"))
    }

    @Test
    fun `non-regulated verse returns null`() {
        assertNull(PingZeChecker.detectForm("春花秋月何时了？往事知多少。")) // 词
        assertNull(PingZeChecker.detectForm("床前明月光")) // 单句
        assertNull(PingZeChecker.detectForm(""))
    }

    // ── 用可控平仄构造 AnnotatedPoem（绕过字典，精确测规则） ──

    private fun buildPoem(vararg lineTones: List<ToneClass>): AnnotatedPoem {
        val annotatedLines = lineTones.mapIndexed { li, tones ->
            val chars = tones.mapIndexed { ci, t ->
                CharMeta(
                    char = ('甲'.code + li * 10 + ci).toChar(),
                    tone = t,
                    isRhymeWord = ci == tones.lastIndex,
                    rhyme = null,
                )
            }
            AnnotatedLine(text = "", chars = chars)
        }
        return AnnotatedPoem(poemId = 0L, lines = annotatedLines, form = "五言绝句")
    }

    @Test
    fun `correct regulated quatrain has no issues`() {
        // 仄起仄收五绝：仄仄平平仄，平平仄仄平。平平平仄仄，仄仄仄平平。
        val poem = buildPoem(
            listOf(OB, OB, L, L, OB),
            listOf(L, L, OB, OB, L),
            listOf(L, L, L, OB, OB),
            listOf(OB, OB, OB, L, L),
        )
        val issues = PingZeChecker.check(poem, "五言绝句")
        assertEquals(emptyList<PingZeIssue>(), issues)
    }

    @Test
    fun `lonely level is flagged`() {
        // 平收句 平平仄仄平 → 首字变仄 → 仄平仄仄平 → 孤平
        val poem = buildPoem(
            listOf(OB, OB, L, L, OB),
            listOf(OB, L, OB, OB, L), // 孤平
            listOf(L, L, L, OB, OB),
            listOf(OB, OB, OB, L, L),
        )
        val lonely = PingZeChecker.check(poem, "五言绝句").first { it.type == IssueType.LONELY_LEVEL }
        assertEquals(1, lonely.lineIndex)
    }

    @Test
    fun `triple level tail is flagged`() {
        val poem = buildPoem(
            listOf(OB, OB, L, L, OB),
            listOf(L, L, OB, OB, L),
            listOf(OB, OB, L, L, L), // 三平尾
            listOf(OB, OB, OB, L, L),
        )
        assertTrue(PingZeChecker.check(poem, "五言绝句").any { it.type == IssueType.TRIPLE_LEVEL_TAIL })
    }

    @Test
    fun `triple oblique tail is flagged`() {
        val poem = buildPoem(
            listOf(OB, OB, L, L, OB),
            listOf(L, L, OB, OB, L),
            listOf(L, L, L, OB, OB),
            listOf(L, L, OB, OB, OB), // 三仄尾
        )
        assertTrue(PingZeChecker.check(poem, "五言绝句").any { it.type == IssueType.TRIPLE_OBLIQUE_TAIL })
    }

    @Test
    fun `mismatch within couplet is flagged`() {
        // 联内两句关键位同平仄 → 失对
        val poem = buildPoem(
            listOf(L, L, OB, OB, L),
            listOf(L, L, OB, OB, L), // 与首句同 → 失对
            listOf(L, L, L, OB, OB),
            listOf(OB, OB, OB, L, L),
        )
        assertTrue(PingZeChecker.check(poem, "五言绝句").any { it.type == IssueType.MISMATCH_IN_COUPLET })
    }

    @Test
    fun `mismatch between couplets is flagged`() {
        // 句2 与句3 关键位应相粘，此处相反 → 失粘
        val poem = buildPoem(
            listOf(OB, OB, L, L, OB),
            listOf(L, L, OB, OB, L),
            listOf(OB, OB, L, L, OB), // 与句2 相反 → 失粘
            listOf(L, L, OB, OB, L),
        )
        assertTrue(PingZeChecker.check(poem, "五言绝句").any { it.type == IssueType.MISMATCH_BETWEEN_COUPLETS })
    }

    @Test
    fun `rhyme mismatch is flagged`() {
        // 手工构造：末字为韵脚，给出不同韵部
        val lines = listOf(
            listOf(OB, OB, L, L, OB),
            listOf(L, L, OB, OB, L),
            listOf(L, L, L, OB, OB),
            listOf(OB, OB, OB, L, L),
        )
        val rhymes = listOf("十二侯", "十二侯", "十二侯", "十六唐") // 第 4 句（偶句）出韵
        val poem = AnnotatedPoem(
            poemId = 0L,
            form = "五言绝句",
            lines = lines.mapIndexed { li, tones ->
                AnnotatedLine(text = "", chars = tones.mapIndexed { ci, t ->
                    CharMeta(
                        char = ('甲'.code + li * 10 + ci).toChar(),
                        tone = t,
                        isRhymeWord = ci == tones.lastIndex,
                        rhyme = rhymes[li],
                    )
                })
            },
        )
        val mismatch = PingZeChecker.check(poem, "五言绝句").filter { it.type == IssueType.RHYME_MISMATCH }
        assertEquals(1, mismatch.size)
        assertEquals(3, mismatch[0].lineIndex)
    }

    @Test
    fun `unknown tones are skipped for couplet checks`() {
        // 关键位含 UNKNOWN 时不应误报失对
        val poem = buildPoem(
            listOf(OB, OB, UNK, L, OB),
            listOf(L, L, OB, OB, L),
            listOf(L, L, L, OB, OB),
            listOf(OB, OB, OB, L, L),
        )
        val issues = PingZeChecker.check(poem, "五言绝句")
        assertTrue(issues.none { it.type == IssueType.MISMATCH_IN_COUPLET || it.type == IssueType.MISMATCH_BETWEEN_COUPLETS })
    }

    private companion object {
        val L = ToneClass.LEVEL
        val OB = ToneClass.OBLIQUE
        val UNK = ToneClass.UNKNOWN
    }
}
