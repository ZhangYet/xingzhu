package com.xingzhun.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingzhun.data.local.PoemDao
import com.xingzhun.data.local.PoemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val poemDao: PoemDao,
) : ViewModel() {

    val poems: StateFlow<List<PoemEntity>> = poemDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { poemDao.delete(id) }
    }
}
