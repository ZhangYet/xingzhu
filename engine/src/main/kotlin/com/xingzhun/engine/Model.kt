package com.xingzhun.engine

import kotlinx.serialization.Serializable

/** 平 / 仄（按《诗韵新编》判定）；UNKNOWN = 字典未收录，待考 */
@Serializable
enum class ToneClass { LEVEL, OBLIQUE, UNKNOWN }

/** 单个字的标注信息 */
@Serializable
data class CharMeta(
    val char: Char,
    val tone: ToneClass,
    /** 《诗韵新编》韵部，如 "七齐" */
    val rhyme: String? = null,
    /** 是否为句末字（韵脚） */
    val isRhymeWord: Boolean = false,
    /** 多音字取判是否存疑 */
    val ambiguous: Boolean = false,
)

/** 一行（一个"句"）及其逐字标注 */
@Serializable
data class AnnotatedLine(
    val text: String,
    val chars: List<CharMeta>,
)

/** 一首诗的完整标注结果 */
@Serializable
data class AnnotatedPoem(
    val poemId: Long,
    val lines: List<AnnotatedLine>,
    /** 体裁：五绝/七绝/五律/七律/古体/词 */
    val form: String,
    /** 附加说明（入声、多音、待考） */
    val annotations: List<String> = emptyList(),
)
