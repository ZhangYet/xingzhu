package com.xingzhu.data.source

import android.content.Context
import com.xingzhu.data.model.PoemSeed
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import kotlinx.serialization.json.Json

/** 内置语料：精选种子（书架初始数据）+ 多集大词库（搜索源，gzip 存储） */
class CorpusLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** 精选 10 首，首次启动自动加入书架 */
    val seeds: List<PoemSeed> by lazy { load(SEED_PATH) }

    /** 搜索词库：诗经/楚辞/全唐诗/宋词/五代/元曲/清，按集 gzip 文件合并（简体） */
    val searchCorpus: List<PoemSeed> by lazy {
        context.assets.list("corpus").orEmpty()
            .filter { it.endsWith(".json.gz") }
            .sorted()
            .flatMap { loadGz("corpus/$it") }
    }

    private fun load(path: String): List<PoemSeed> {
        val text = context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return json.decodeFromString<List<PoemSeed>>(text)
    }

    private fun loadGz(path: String): List<PoemSeed> {
        val text = GZIPInputStream(context.assets.open(path))
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return json.decodeFromString<List<PoemSeed>>(text)
    }

    private companion object {
        const val SEED_FILE = "poems.json"
        const val SEED_PATH = "corpus/$SEED_FILE"
    }
}
