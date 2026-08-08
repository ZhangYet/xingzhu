package com.xingzhu.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingzhu.data.local.PoemEntity
import com.xingzhu.data.model.PoemSeed
import com.xingzhu.data.repository.PoemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
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
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun add(seed: PoemSeed) {
        viewModelScope.launch { repository.addToLibrary(seed) }
    }
}
