package com.xingzhun.data.source

import android.content.Context
import com.xingzhun.data.model.PoemSeed
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.serialization.json.Json

/** 内置语料：精选种子（书架初始数据）+ 大词库（搜索源） */
class CorpusLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** 精选 10 首，首次启动自动加入书架 */
    val seeds: List<PoemSeed> by lazy { load(SEED_PATH) }

    /** 搜索词库（唐诗三百首 + 宋词三百首，繁体已转简体） */
    val searchCorpus: List<PoemSeed> by lazy { load(SEARCH_PATH) }

    private fun load(path: String): List<PoemSeed> {
        val text = context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return json.decodeFromString<List<PoemSeed>>(text)
    }

    private companion object {
        const val SEED_PATH = "corpus/poems.json"
        const val SEARCH_PATH = "corpus/search_poems.json"
    }
}
