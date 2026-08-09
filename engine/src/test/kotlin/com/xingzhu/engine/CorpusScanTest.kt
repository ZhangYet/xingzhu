package com.xingzhu.engine

import java.io.File
import java.util.zip.GZIPInputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * 全语料扫描（诊断用，非业务单测）：
 * 跑完 7.6 万首，统计 待考字 / 韵部映射缺失 / 入声注释与今读声调不符 / 入声字韵部分布，
 * 把问题报告写到 /tmp/corpus_scan_report.txt。
 *
 * 运行：./gradlew :engine:test --tests "com.xingzhu.engine.CorpusScanTest"
 * 语料路径可用 -Dcorpus.dir=... 覆盖，默认指向 app/src/main/assets/corpus
 */
class CorpusScanTest {

    @Serializable
    private data class CorpusPoem(
        val title: String = "",
        val author: String = "",
        val dynasty: String? = null,
        val form: String = "",
        val content: String = "",
    )

    @Test
    fun scanCorpus() {
        val dir = System.getProperty("corpus.dir") ?: "../app/src/main/assets/corpus"
        val dict = ShiYunXinBianDictionary.fromClasspath()
        val engine = PingZeEngine(dict)
        val json = Json { ignoreUnknownKeys = true }

        val poems = File(dir).listFiles { f -> f.name.endsWith(".json.gz") }
            .sortedBy { it.name }
            .flatMap { file ->
                GZIPInputStream(file.inputStream()).bufferedReader(Charsets.UTF_8).use { br ->
                    json.decodeFromString<List<CorpusPoem>>(br.readText())
                }
            }
        println("语料加载: ${poems.size} 首")

        // 统计
        val unknown = sortedMapOf<Char, Int>()                 // 待考字（仅汉字）
        val legitPunct = sortedMapOf<Char, Int>()              // 合法句读标点
        val pollution = sortedMapOf<Char, Int>()               // 编辑性污染（括号/引号/□/半角）
        val rhymeNull = mutableListOf<Pair<String, String>>()  // 韵脚字 rhyme==null: (字, 诗题)
        val rushuMismatch = sortedMapOf<Char, Int>()           // 入声字注释与今读声调不符
        val rushuRhyme = sortedMapOf<String, Int>()            // 入声字 → 韵母映射出的韵部分布
        val tone3tone4 = mutableListOf<Char>()                 // 入声字注释错误的示例字
        var annotated = 0
        var errors = 0
        val errorSamples = mutableListOf<String>()
        val legitSet = setOf('。', '，', '、', '；', '：', '？', '！')

        fun isHanzi(c: Char) = c in '\u4E00'..'\u9FFF'

        for (poem in poems) {
            try {
                val ann = engine.annotatePoem(poemId = 0L, form = poem.form, contentText = poem.content)
                annotated++
                for (line in ann.lines) {
                    for (meta in line.chars) {
                        if (isHanzi(meta.char)) {
                            if (meta.tone == ToneClass.UNKNOWN) {
                                unknown[meta.char] = (unknown[meta.char] ?: 0) + 1
                            }
                            if (meta.isRhymeWord && meta.rhyme == null && dict.lookup(meta.char) != null) {
                                rhymeNull.add(meta.char.toString() to poem.title)
                            }
                        } else if (meta.char in legitSet) {
                            legitPunct[meta.char] = (legitPunct[meta.char] ?: 0) + 1
                        } else {
                            pollution[meta.char] = (pollution[meta.char] ?: 0) + 1
                        }
                    }
                }
                for (note in ann.annotations) {
                    // 「X」为入声字（今读平声，按仄计） 或 「X」为入声字（诗韵新编判仄声）
                    if (note.contains("入声字")) {
                        val ch = note.trim().removePrefix("「").firstOrNull() ?: continue
                        val tone = dict.rawTone(ch) ?: continue
                        val saysLevel = note.contains("今读平声")
                        val actuallyLevel = tone in 1..2
                        if (saysLevel != actuallyLevel) {
                            rushuMismatch[ch] = (rushuMismatch[ch] ?: 0) + 1
                            if (tone3tone4.size < 40) tone3tone4.add(ch)
                        }
                    }
                }
            } catch (e: Exception) {
                errors++
                if (errorSamples.size < 10) errorSamples.add("${poem.title}: ${e.message}")
            }
        }

        // 入声字韵部分布：直接扫字典里的入声字
        for ((char, rhyme) in dict.allRhymesForScan()) {
            if (char in dict.rushuSetForScan()) {
                rushuRhyme[rhyme ?: "NULL"] = (rushuRhyme[rhyme ?: "NULL"] ?: 0) + 1
            }
        }

        val sb = StringBuilder()
        sb.appendLine("===== 全语料扫描报告 =====")
        sb.appendLine("诗数: $poems.size  标注成功: $annotated  异常: $errors")
        if (errorSamples.isNotEmpty()) sb.appendLine("异常示例: ${errorSamples.take(5)}")

        sb.appendLine()
        sb.appendLine("【1a】待考字（字典未收录，仅汉字）: ${unknown.values.sum()} 处 / ${unknown.size} 字")
        sb.appendLine("  示例: ${unknown.keys.take(40).joinToString("")}")

        sb.appendLine()
        sb.appendLine("【1b】编辑性污染（括号/引号/□/半角等）: ${pollution.values.sum()} 处 / ${pollution.size} 种")
        sb.appendLine("  示例: ${pollution.entries.sortedByDescending { it.value }.take(20).joinToString(" ") { "${it.key}(${it.value})" }}")
        sb.appendLine("  合法句读标点: ${legitPunct.values.sum()} 处")

        sb.appendLine()
        sb.appendLine("【2】韵脚字 rhyme 缺失: ${rhymeNull.size}")
        sb.appendLine("  示例: ${rhymeNull.take(20)}")

        sb.appendLine()
        sb.appendLine("【3】入声字注释『今读平声』但实际今读仄声: ${rushuMismatch.values.sum()} 处 / ${rushuMismatch.size} 字")
        sb.appendLine("  示例字: ${tone3tone4.distinct().joinToString("")}")

        sb.appendLine()
        sb.appendLine("【4】入声字 → 韵部（当前按普通话韵母映射）分布:")
        rushuRhyme.toSortedMap().forEach { (k, v) -> sb.appendLine("  $k: $v") }

        sb.appendLine()
        sb.appendLine("【5】多音字（ambiguous）出现示例:（供人工抽查）")
        val amb = dict.allRhymesForScan().keys
            .filter { dict.lookup(it)?.ambiguous == true }
            .take(40)
        sb.appendLine("  ${amb.joinToString("")}")

        File("/tmp/corpus_scan_report.txt").writeText(sb.toString(), Charsets.UTF_8)
        println(sb.toString())
    }
}
