package com.xingzhun.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSplitterTest {

    @Test
    fun `split by punctuation`() {
        val text = "床前明月光，疑是地上霜。举头望明月，低头思故乡。"
        val lines = TextSplitter.splitSentences(text)
        assertEquals(4, lines.size)
        assertEquals("床前明月光，", lines[0])
        assertEquals("低头思故乡。", lines[3])
    }

    @Test
    fun `no trailing punctuation keeps last segment`() {
        val text = "白日依山尽，黄河入海流。欲穷千里目"
        val lines = TextSplitter.splitSentences(text)
        assertEquals(3, lines.size)
        assertEquals("欲穷千里目", lines[2])
    }

    @Test
    fun `blank text yields empty list`() {
        assertEquals(0, TextSplitter.splitSentences("  ").size)
    }
}

class ShiYunXinBianDictionaryTest {

    private val dict = ShiYunXinBianDictionary.fromClasspath()

    private fun entry(char: Char): DictionaryEntry {
        val e = dict.lookup(char)
        assertNotNull("「$char」应在字典中", e)
        return checkNotNull(e)
    }

    @Test
    fun `rhyme groups by mandarin final`() {
        assertEquals("十六唐", entry('光').rhyme)   // guāng
        assertEquals("十四寒", entry('前').rhyme)   // qián
        assertEquals("四皆", entry('月').rhyme)     // yuè
        assertEquals("十一鱼", entry('语').rhyme)   // yǔ
        assertEquals("五支", entry('思').rhyme)     // sī
        assertEquals("五支", entry('日').rhyme)     // rì
        assertEquals("十六唐", entry('乡').rhyme)   // xiāng
        assertEquals("十八东", entry('宫').rhyme)   // gōng
        assertEquals("十二侯", entry('楼').rhyme)   // lóu
    }

    @Test
    fun `tone classification by pinyin tone`() {
        assertEquals(ToneClass.LEVEL, entry('光').toneClass)   // 阴平
        assertEquals(ToneClass.LEVEL, entry('前').toneClass)   // 阳平
        assertEquals(ToneClass.OBLIQUE, entry('是').toneClass) // 去声
        assertEquals(ToneClass.OBLIQUE, entry('上').toneClass) // 上声
    }

    @Test
    fun `rushu chars are oblique and flagged`() {
        for (char in listOf('白', '日', '月', '入', '目', '急', '雪')) {
            val e = entry(char)
            assertTrue("「$char」应为入声字", e.isRushu)
            assertEquals("「$char」入声应为仄", ToneClass.OBLIQUE, e.toneClass)
        }
    }

    @Test
    fun `polyphonic chars are marked ambiguous`() {
        assertTrue(entry('上').ambiguous)
        assertFalse(entry('前').ambiguous)
    }

    @Test
    fun `canonicalize y w and u with umlaut`() {
        assertEquals("üe", ShiYunXinBianDictionary.canonicalize("yue"))
        assertEquals("üan", ShiYunXinBianDictionary.canonicalize("yuan"))
        assertEquals("ün", ShiYunXinBianDictionary.canonicalize("yun"))
        assertEquals("jü", ShiYunXinBianDictionary.canonicalize("ju"))
        assertEquals("lü", ShiYunXinBianDictionary.canonicalize("lü"))
        assertEquals("ueng", ShiYunXinBianDictionary.canonicalize("weng"))
        assertEquals("iong", ShiYunXinBianDictionary.canonicalize("yong"))
    }
}

class PingZeEngineTest {

    private val engine = PingZeEngine()

    @Test
    fun `annotate jing-ye-si`() {
        val poem = engine.annotatePoem(
            poemId = 1L,
            form = "五言绝句",
            contentText = "床前明月光，疑是地上霜。举头望明月，低头思故乡。",
        )
        assertEquals(4, poem.lines.size)

        val line0 = poem.lines[0]
        val guang = line0.chars.first { it.char == '光' }
        assertTrue(guang.isRhymeWord)
        assertEquals(ToneClass.LEVEL, guang.tone)
        assertEquals("十六唐", guang.rhyme)

        val shuang = poem.lines[1].chars.first { it.char == '霜' }
        assertTrue(shuang.isRhymeWord)
        assertEquals("十六唐", shuang.rhyme)

        val yue = poem.lines[2].chars.first { it.char == '月' }
        assertTrue(yue.isRhymeWord)
        assertEquals(ToneClass.OBLIQUE, yue.tone)
        assertEquals("四皆", yue.rhyme)

        assertTrue(poem.annotations.any { it.contains("入声") })
    }

    @Test
    fun `annotate deng-guan-quelou`() {
        val poem = engine.annotatePoem(
            poemId = 2L,
            form = "五言绝句",
            contentText = "白日依山尽，黄河入海流。欲穷千里目，更上一层楼。",
        )
        val bai = poem.lines[0].chars.first { it.char == '白' }
        assertEquals(ToneClass.OBLIQUE, bai.tone)

        val mu = poem.lines[2].chars.first { it.char == '目' }
        assertTrue(mu.isRhymeWord)
        assertEquals(ToneClass.OBLIQUE, mu.tone)

        val lou = poem.lines[3].chars.first { it.char == '楼' }
        assertTrue(lou.isRhymeWord)
        assertEquals(ToneClass.LEVEL, lou.tone)
        assertEquals("十二侯", lou.rhyme)
    }

    @Test
    fun `unfound char is unknown`() {
        val poem = engine.annotatePoem(poemId = 3L, form = "五绝", contentText = "床前明\uD840\uDC00光。")
        // 增补平面字拆为代理对，字典未收录 → 待考
        val weird = poem.lines[0].chars.first { it.char.code in 0xD800..0xDFFF }
        assertEquals(ToneClass.UNKNOWN, weird.tone)
        assertTrue(poem.annotations.any { it.contains("待考") })
    }
}
