package com.xingzhun.data.repository

import com.xingzhun.data.local.PoemDao
import com.xingzhun.data.local.PoemEntity
import com.xingzhun.data.model.PoemSeed
import com.xingzhun.data.source.CorpusLoader
import com.xingzhun.engine.AnnotatedPoem
import com.xingzhun.engine.PingZeEngine
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

class PoemRepository @Inject constructor(
    private val poemDao: PoemDao,
    corpusLoader: CorpusLoader,
    private val engine: PingZeEngine,
) {
    private val corpus: List<PoemSeed> by lazy { corpusLoader.load() }
    private val json = Json { ignoreUnknownKeys = true }

    fun observeLibrary(): Flow<List<PoemEntity>> = poemDao.observeAll()

    fun observePoem(id: Long): Flow<PoemEntity?> = poemDao.observeById(id)

    fun searchCorpus(query: String): List<PoemSeed> {
        val q = query.trim()
        if (q.isEmpty()) return corpus
        return corpus.filter { it.title.contains(q) || it.author.contains(q) }
    }

    fun isInLibrary(seed: PoemSeed, library: List<PoemEntity>): Boolean =
        library.any { it.title == seed.title && it.author == seed.author }

    /** 加入书架，并同步生成平仄/韵脚标注缓存 */
    suspend fun addToLibrary(seed: PoemSeed): Long {
        val id = poemDao.insert(
            PoemEntity(
                title = seed.title,
                author = seed.author,
                dynasty = seed.dynasty,
                form = seed.form,
                contentText = seed.content,
            ),
        )
        val annotated = runCatching {
            engine.annotatePoem(poemId = id, form = seed.form, contentText = seed.content)
        }.getOrNull()
        poemDao.updateAnnotation(id, annotated?.let { json.encodeToString(AnnotatedPoem.serializer(), it) })
        return id
    }

    suspend fun delete(id: Long) = poemDao.delete(id)
}
