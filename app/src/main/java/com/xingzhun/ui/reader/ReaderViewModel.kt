package com.xingzhun.ui.reader

import androidx.lifecycle.ViewModel
import com.xingzhun.data.local.PoemEntity
import com.xingzhun.data.repository.PoemRepository
import com.xingzhun.engine.AnnotatedPoem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

data class ReaderPoem(
    val entity: PoemEntity?,
    val annotated: AnnotatedPoem?,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: PoemRepository,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    fun observe(poemId: Long): Flow<ReaderPoem> =
        repository.observePoem(poemId).map { poem ->
            val annotated = poem?.annotationJson?.let { raw ->
                runCatching { json.decodeFromString<AnnotatedPoem>(raw) }.getOrNull()
            }
            ReaderPoem(entity = poem, annotated = annotated)
        }
}
