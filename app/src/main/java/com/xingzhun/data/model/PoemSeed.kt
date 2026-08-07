package com.xingzhun.data.model

import kotlinx.serialization.Serializable

/** 内置语料中的一首诗（assets/corpus/poems.json 的一项） */
@Serializable
data class PoemSeed(
    val title: String,
    val author: String,
    val dynasty: String,
    val form: String,
    val content: String,
)
