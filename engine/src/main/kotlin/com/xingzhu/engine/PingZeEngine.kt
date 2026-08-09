package com.xingzhu.engine

/**
 * 按标点把诗文切分为"句"（句末字即韵脚）。
 * 标点保留在句中，返回每句的完整文本。
 */
object TextSplitter {

    private val SENTENCE_BOUNDARY = Regex("[。！？，；：、]")

    fun splitSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        var start = 0
        var i = 0
        while (i < text.length) {
            if (SENTENCE_BOUNDARY.matches(text[i].toString()) && i < text.length - 1) {
                // 句末标点若在文本中间，则句到此为止
                result.add(text.substring(start, i + 1))
                start = i + 1
            }
            i++
        }
        if (start < text.length) {
            result.add(text.substring(start))
        }
        return result
    }
}

private val PUNCTUATION = setOf('。', '！', '？', '，', '；', '：', '、')

/**
 * 标注引擎：按《诗韵新编》逐字标注平仄与韵部，句末非标点字标注为韵脚。
 */
class PingZeEngine(
    private val dictionary: ShiYunXinBianDictionary = ShiYunXinBianDictionary.fromClasspath(),
) {

    fun annotatePoem(poemId: Long, form: String, contentText: String): AnnotatedPoem {
        val lines = TextSplitter.splitSentences(contentText)
        val notes = mutableListOf<String>()
        val annotatedLines = lines.map { line ->
            val rhymeIndex = line.indexOfLast { it !in PUNCTUATION }
            val chars = line.mapIndexed { index, c ->
                if (c in PUNCTUATION) {
                    CharMeta(char = c, tone = ToneClass.UNKNOWN, isRhymeWord = false)
                } else {
                    annotateChar(c, index == rhymeIndex, notes)
                }
            }
            AnnotatedLine(text = line, chars = chars)
        }
        return AnnotatedPoem(
            poemId = poemId,
            form = form,
            lines = annotatedLines,
            annotations = notes,
        )
    }

    private fun annotateChar(c: Char, isRhymeWord: Boolean, notes: MutableList<String>): CharMeta {
        val entry = dictionary.lookup(c)
        if (entry == null) {
            notes.addIfAbsent("「$c」字典未收录，标为待考")
            return CharMeta(char = c, tone = ToneClass.UNKNOWN, isRhymeWord = isRhymeWord)
        }
        if (entry.isRushu) {
            // 入声字今读平声 → 说明按仄计；今读仄声 → 直接说明按诗韵新编判仄
            if (entry.modernTone in 1..2) {
                notes.addIfAbsent("「$c」为入声字（今读平声，按仄计）")
            } else {
                notes.addIfAbsent("「$c」为入声字（诗韵新编判仄声）")
            }
        }
        return CharMeta(
            char = c,
            tone = entry.toneClass,
            rhyme = entry.rhyme,
            isRhymeWord = isRhymeWord,
            ambiguous = entry.ambiguous,
        )
    }

    private fun MutableList<String>.addIfAbsent(text: String) {
        if (text !in this) add(text)
    }
}
