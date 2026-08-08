package com.xingzhu.data.local

import net.sourceforge.pinyin4j.PinyinHelper

/** 拼音工具：标题/作者首字母（大写），用于书架按首字母排序 */
object PinyinUtil {

    /** "静夜思" → "JYS"；非汉字原样大写 */
    fun firstLetters(text: String): String = buildString {
        for (c in text) {
            val pinyin = PinyinHelper.toHanyuPinyinStringArray(c)
            if (pinyin != null && pinyin.isNotEmpty() && pinyin[0].isNotEmpty()) {
                append(pinyin[0][0].uppercaseChar())
            } else {
                append(c.uppercaseChar())
            }
        }
    }
}
