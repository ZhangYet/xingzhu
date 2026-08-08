package com.xingzhu.data.repository

import com.xingzhu.data.local.PinyinUtil
import com.xingzhu.data.local.PoemDao
import com.xingzhu.data.local.PoemEntity
import com.xingzhu.data.model.PoemSeed
import com.xingzhu.data.source.CorpusLoader
import com.xingzhu.engine.AnnotatedPoem
import com.xingzhu.engine.PingZeEngine
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

class PoemRepository @Inject constructor(
    private val poemDao: PoemDao,
    private val corpusLoader: CorpusLoader,
    private val engine: PingZeEngine,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeLibrary(): Flow<List<PoemEntity>> = poemDao.observeAll()

    fun observePoem(id: Long): Flow<PoemEntity?> = poemDao.observeById(id)

    /** 首次启动：把精选种子自动加入书架；并为旧数据回填拼音 */
    suspend fun ensureCorpusSeeded() {
        if (poemDao.count() == 0L) {
            corpusLoader.seeds.forEach { addToLibrary(it) }
        }
        backfillPinyin()
    }

    /** 回填缺失的拼音（Room v1→v2 迁移后旧行拼音为空） */
    suspend fun backfillPinyin() {
        poemDao.poemsMissingPinyin().forEach { poem ->
            poemDao.updatePinyin(
                poem.id,
                PinyinUtil.firstLetters(poem.title),
                PinyinUtil.firstLetters(poem.author),
            )
        }
    }

    /** 按题目/作者/全文搜索，取前 50 条 */
    fun searchCorpus(query: String): List<PoemSeed> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return corpusLoader.searchCorpus.asSequence()
            .filter { it.title.contains(q) || it.author.contains(q) || it.content.contains(q) }
            .take(50)
            .toList()
    }

    fun isInLibrary(seed: PoemSeed, library: List<PoemEntity>): Boolean =
        library.any { it.title == seed.title && it.author == seed.author }

    /** 加入书架（同题目+作者已存在则不重复插入），并同步生成平仄/韵脚标注缓存与拼音 */
    suspend fun addToLibrary(seed: PoemSeed): Long {
        poemDao.findByTitleAuthor(seed.title, seed.author)?.let { return it.id }
        val id = poemDao.insert(
            PoemEntity(
                title = seed.title,
                author = seed.author,
                dynasty = seed.dynasty,
                form = seed.form,
                contentText = seed.content,
                titlePinyin = PinyinUtil.firstLetters(seed.title),
                authorPinyin = PinyinUtil.firstLetters(seed.author),
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
