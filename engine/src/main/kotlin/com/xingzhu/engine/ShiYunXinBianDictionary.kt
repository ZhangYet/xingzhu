package com.xingzhu.engine

/**
 * 《诗韵新编》字典。
 *
 * 数据来源：
 * - kxhc1983.txt：新华字典 1983 拼音（带声调）字表（mozillazg/pinyin-data）
 * - rushu.txt：入声字表（取自平水韵入声十七韵的并集）
 *
 * 平仄判定：阴平(1)/阳平(2) → 平；上(3)/去(4) → 仄；入声字 → 仄；
 * 韵部判定：按普通话韵母映射到《诗韵新编》十八韵。
 */
data class DictionaryEntry(
    val toneClass: ToneClass,
    val rhyme: String?,
    val isRushu: Boolean,
    val ambiguous: Boolean,
    /** 现代普通话声调 1-4；0 = 轻声/无调 */
    val modernTone: Int,
)

class ShiYunXinBianDictionary private constructor(
    private val entries: Map<Char, CharEntry>,
    private val rushuSet: Set<Char>,
) {

    fun lookup(char: Char): DictionaryEntry? {
        val entry = entries[char] ?: return null
        val isRushu = char in rushuSet
        val toneClass = when {
            isRushu -> ToneClass.OBLIQUE
            entry.tone == 0 -> ToneClass.LEVEL // 轻声按平处理（诗作中罕见）
            entry.tone <= 2 -> ToneClass.LEVEL
            else -> ToneClass.OBLIQUE
        }
        return DictionaryEntry(
            toneClass = toneClass,
            rhyme = entry.rhyme,
            isRushu = isRushu,
            ambiguous = entry.ambiguous,
            modernTone = entry.tone,
        )
    }

    /** 现代普通话声调 1-4/0；未收录返回 null（诊断用，非业务接口） */
    internal fun rawTone(char: Char): Int? = entries[char]?.tone

    /** 诊断用：全部已收录字的韵部（按当前映射） */
    internal fun allRhymesForScan(): Map<Char, String?> = entries.mapValues { it.value.rhyme }

    /** 诊断用：入声字集合 */
    internal fun rushuSetForScan(): Set<Char> = rushuSet

    private data class CharEntry(
        val tone: Int,          // 1-4；0 = 轻声/无调
        val rhyme: String?,     // 诗韵新编韵部
        val ambiguous: Boolean, // 多音字
    )

    companion object {
        private const val KXHC_RES = "/kxhc1983.txt"
        private const val RUSHU_RES = "/rushu.txt"

        private val TONE_MARKS = mapOf(
            'ā' to ('a' to 1), 'á' to ('a' to 2), 'ǎ' to ('a' to 3), 'à' to ('a' to 4),
            'ē' to ('e' to 1), 'é' to ('e' to 2), 'ě' to ('e' to 3), 'è' to ('e' to 4),
            'ī' to ('i' to 1), 'í' to ('i' to 2), 'ǐ' to ('i' to 3), 'ì' to ('i' to 4),
            'ō' to ('o' to 1), 'ó' to ('o' to 2), 'ǒ' to ('o' to 3), 'ò' to ('o' to 4),
            'ū' to ('u' to 1), 'ú' to ('u' to 2), 'ǔ' to ('u' to 3), 'ù' to ('u' to 4),
            'ǖ' to ('ü' to 1), 'ǘ' to ('ü' to 2), 'ǚ' to ('ü' to 3), 'ǜ' to ('ü' to 4),
        )

        // 五支韵：zhi/chi/shi/ri/zi/ci/si 的 "i" 是舌尖元音，独立成韵
        private val ZHI_CHI_SHI_RI = setOf("zhi", "chi", "shi", "ri", "zi", "ci", "si")

        // 多音字在诗词语境中的常用读音（kXHC1983 首读音可能不适合诗词）
        private val POETRY_OVERRIDES: Map<Char, String> = mapOf(
            '思' to "sī",   // 低"头思"故乡（sāi 是"于思"义）
            '更' to "gèng", // "更"上一层楼（去声）
            '觉' to "jué",  // 春眠不"觉"晓（入声）
            '雀' to "què",  // 登鹳"雀"楼（入声）
            '还' to "huán", // "还"乡
            '行' to "xíng",
            '长' to "cháng",
            '重' to "chóng",
            '中' to "zhōng",
            '空' to "kōng",
            '朝' to "zhāo",
            '看' to "kān",
            '应' to "yīng",
            '兴' to "xīng",
            '分' to "fēn",
            '当' to "dāng",
            '相' to "xiāng",
            '将' to "jiāng",
            '为' to "wéi",
            '间' to "jiān",
            '教' to "jiāo",
            '曲' to "qū",
            '绿' to "lǜ",
            '调' to "tiáo",
            '胜' to "shèng",
            '处' to "chù",
            '地' to "dì",   // 诗作中"地上/天地"读 dì（去声），kXHC 首选是助词轻声 de
            '育' to "yù",   // 诗作中"养育/孕育"读 yù；kXHC 首选是"生育"的 yō（无对应韵母）
        )

        private val INITIALS = listOf("zh", "ch", "sh") +
            "bpmfdtnlgkhjqxrzcs".map { it.toString() }

        fun fromClasspath(): ShiYunXinBianDictionary {
            val kxhcStream = checkNotNull(
                object {}.javaClass.getResourceAsStream(KXHC_RES),
            ) { "missing $KXHC_RES" }
            val rushuStream = checkNotNull(
                object {}.javaClass.getResourceAsStream(RUSHU_RES),
            ) { "missing $RUSHU_RES" }
            return build(kxhcStream, rushuStream)
        }

        fun build(kxhc: java.io.InputStream, rushu: java.io.InputStream): ShiYunXinBianDictionary {
            val rushuSet = rushu.bufferedReader(Charsets.UTF_8).use { it.readText().toSet() }

            val entries = linkedMapOf<Char, CharEntry>()
            kxhc.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    val match = KXHC_LINE.matchEntire(line.trim())
                        ?: continue
                    val codePoint = match.groupValues[1].toInt(16)
                    val char = codePoint.toChar()
                    val readings = match.groupValues[2].split(',')
                    val first = readings.first()
                    val (syllable, tone) = normalizeReading(first)
                    entries[char] = CharEntry(
                        tone = tone,
                        rhyme = rhymeGroup(syllable),
                        ambiguous = readings.size > 1,
                    )
                }
            }
            for ((char, reading) in POETRY_OVERRIDES) {
                val (syllable, tone) = normalizeReading(reading)
                entries[char] = CharEntry(
                    tone = tone,
                    rhyme = rhymeGroup(syllable),
                    ambiguous = true,
                )
            }
            return ShiYunXinBianDictionary(entries, rushuSet)
        }

        private val KXHC_LINE = Regex("""^U\+([0-9A-Fa-f]+):\s*(\S+)\s*#\s*\S+$""")

        /** 去除声调符号，返回（规范音节，声调 1-4/0） */
        internal fun normalizeReading(raw: String): Pair<String, Int> {
            var tone = 0
            val sb = StringBuilder()
            for (c in raw) {
                val t = TONE_MARKS[c]
                if (t != null) {
                    sb.append(t.first)
                    if (tone == 0) tone = t.second
                } else {
                    sb.append(c)
                }
            }
            return canonicalize(sb.toString()) to tone
        }

        /** y/w 首转换为元音等价形式；j/q/x/u 与 y+u 的 u 视为 ü */
        internal fun canonicalize(s: String): String {
            return when {
                s.startsWith("yuan") -> "üan" + s.removePrefix("yuan")
                s.startsWith("yue") -> "üe" + s.removePrefix("yue")
                s.startsWith("yun") -> "ün" + s.removePrefix("yun")
                s.startsWith("yu") -> "ü" + s.removePrefix("yu")
                s.startsWith("ying") -> "ing" + s.removePrefix("ying")
                s.startsWith("yin") -> "in" + s.removePrefix("yin")
                s.startsWith("yi") -> "i" + s.removePrefix("yi")
                s.startsWith("yong") -> "iong" + s.removePrefix("yong")
                s.startsWith("you") -> "iou" + s.removePrefix("you")
                s.startsWith("yang") -> "iang" + s.removePrefix("yang")
                s.startsWith("yan") -> "ian" + s.removePrefix("yan")
                s.startsWith("yao") -> "iao" + s.removePrefix("yao")
                s.startsWith("ya") -> "ia" + s.removePrefix("ya")
                s.startsWith("ye") -> "ie" + s.removePrefix("ye")
                s.startsWith("weng") -> "ueng" + s.removePrefix("weng")
                s.startsWith("wang") -> "uang" + s.removePrefix("wang")
                s.startsWith("wan") -> "uan" + s.removePrefix("wan")
                s.startsWith("wai") -> "uai" + s.removePrefix("wai")
                s.startsWith("wen") -> "uen" + s.removePrefix("wen")
                s.startsWith("wei") -> "uei" + s.removePrefix("wei")
                s.startsWith("wo") -> "uo" + s.removePrefix("wo")
                s.startsWith("wa") -> "ua" + s.removePrefix("wa")
                s.startsWith("wu") -> "u" + s.removePrefix("wu")
                s.startsWith("j") && s.length >= 2 && s[1] == 'u' ->
                    "jü" + s.removePrefix("ju")
                s.startsWith("q") && s.length >= 2 && s[1] == 'u' ->
                    "qü" + s.removePrefix("qu")
                s.startsWith("x") && s.length >= 2 && s[1] == 'u' ->
                    "xü" + s.removePrefix("xu")
                // 声母后的拼音简写：iu→iou（十二侯）、ui→uei（八微）、un→uen（十五痕）
                s.endsWith("iu") && s.length > 2 -> s.dropLast(2) + "iou"
                s.endsWith("ui") && s.length > 2 -> s.dropLast(2) + "uei"
                s.endsWith("un") && s.length > 2 -> s.dropLast(2) + "uen"
                else -> s
            }
        }

        /** 声母剥离后映射韵部 */
        internal fun rhymeGroup(syllable: String): String? {
            if (syllable in ZHI_CHI_SHI_RI) return "五支"
            val final = stripInitial(syllable)
            return when (final) {
                "a", "ia", "ua" -> "一麻"
                "o", "uo" -> "二波"
                "yo" -> "二波" // 语气词"哟 yō"，近似归二波，避免韵部缺失
                "e" -> "三歌"
                "ie", "üe" -> "四皆"
                "i" -> "七齐"
                "er" -> "六儿"
                "ei", "uei" -> "八微"
                "ai", "uai" -> "九开"
                "u" -> "十姑"
                "ü" -> "十一鱼"
                "ou", "iou" -> "十二侯"
                "ao", "iao" -> "十三豪"
                "an", "ian", "uan", "üan" -> "十四寒"
                "en", "in", "uen", "ün" -> "十五痕"
                "ang", "iang", "uang" -> "十六唐"
                "eng", "ing", "ueng" -> "十七庚"
                "ong", "iong" -> "十八东"
                else -> null
            }
        }

        private fun stripInitial(syllable: String): String {
            for (initial in INITIALS) {
                if (syllable.length > initial.length && syllable.startsWith(initial)) {
                    return syllable.removePrefix(initial)
                }
            }
            return syllable
        }
    }
}
