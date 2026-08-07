package com.xingzhun.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingzhun.data.local.PoemEntity
import com.xingzhun.data.model.PoemSeed
import com.xingzhun.data.repository.PoemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PoemSearchItem(
    val seed: PoemSeed,
    val inLibrary: Boolean,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class AddViewModel @Inject constructor(
    private val repository: PoemRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val library: StateFlow<List<PoemEntity>> = repository.observeLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val results: StateFlow<List<PoemSearchItem>> =
        combine(query.debounce(300).map { it.trim() }, library) { q, lib ->
            repository.searchCorpus(q).map { seed ->
                PoemSearchItem(seed = seed, inLibrary = repository.isInLibrary(seed, lib))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun add(seed: PoemSeed) {
        viewModelScope.launch { repository.addToLibrary(seed) }
    }

    fun addManual(title: String, author: String, dynasty: String, form: String, content: String): Boolean {
        val t = title.trim()
        val c = content.trim()
        if (t.isEmpty() || c.isEmpty()) return false
        viewModelScope.launch {
            repository.addManual(t, author.trim(), dynasty.trim(), form, c)
        }
        return true
    }
}
