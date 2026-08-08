package com.xingzhu.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingzhu.data.local.PoemDao
import com.xingzhu.data.local.PoemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder(val label: String) {
    ADDED_NEWEST("添加时间 新→旧"),
    ADDED_OLDEST("添加时间 旧→新"),
    TITLE_ABC("标题首字母"),
    AUTHOR_ABC("作者首字母"),
    DYNASTY("朝代"),
    FORM("体裁"),
}

data class LibraryUiState(
    val poems: List<PoemEntity> = emptyList(),
    val sortOrder: SortOrder = SortOrder.ADDED_NEWEST,
    val groupByAuthor: Boolean = false,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val poemDao: PoemDao,
) : ViewModel() {

    private val sortOrder = MutableStateFlow(SortOrder.ADDED_NEWEST)
    private val groupByAuthor = MutableStateFlow(false)

    val uiState: StateFlow<LibraryUiState> =
        combine(poemDao.observeAll(), sortOrder, groupByAuthor) { poems, sort, group ->
            LibraryUiState(poems = sortPoems(poems, sort), sortOrder = sort, groupByAuthor = group)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun setSortOrder(order: SortOrder) {
        sortOrder.value = order
    }

    fun setGroupByAuthor(group: Boolean) {
        groupByAuthor.value = group
    }

    fun delete(id: Long) {
        viewModelScope.launch { poemDao.delete(id) }
    }

    private fun sortPoems(poems: List<PoemEntity>, sort: SortOrder): List<PoemEntity> = when (sort) {
        SortOrder.ADDED_NEWEST -> poems.sortedByDescending { it.addedAt }
        SortOrder.ADDED_OLDEST -> poems.sortedBy { it.addedAt }
        SortOrder.TITLE_ABC -> poems.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.titlePinyin })
        SortOrder.AUTHOR_ABC -> poems.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.authorPinyin })
        SortOrder.DYNASTY -> poems.sortedBy { rankOf(it.dynasty, dynastyOrder) }
        SortOrder.FORM -> poems.sortedBy { rankOf(it.form, formOrder) }
    }

    private fun rankOf(value: String?, order: List<String>): Int {
        val idx = order.indexOf(value ?: "")
        return if (idx < 0) order.size else idx
    }

    private val dynastyOrder = listOf("周", "先秦", "汉", "唐", "五代", "宋", "元", "明", "清")
    private val formOrder = listOf(
        "诗经", "楚辞", "乐府诗", "四言诗", "五言绝句", "五言律诗", "七言绝句", "七言律诗", "古体诗", "词", "曲",
    )
}
