package com.xingzhun.data.source

import android.content.Context
import com.xingzhun.data.model.PoemSeed
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.serialization.json.Json

/** 内置语料（离线，v1 共 10 首） */
class CorpusLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun load(): List<PoemSeed> {
        val text = context.assets.open(CORPUS_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return Json { ignoreUnknownKeys = true }.decodeFromString<List<PoemSeed>>(text)
    }

    private companion object {
        const val CORPUS_PATH = "corpus/poems.json"
    }
}
