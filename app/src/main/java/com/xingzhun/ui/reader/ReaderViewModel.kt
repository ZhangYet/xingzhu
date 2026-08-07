package com.xingzhun.ui.reader

import androidx.lifecycle.ViewModel
import com.xingzhun.data.local.PoemEntity
import com.xingzhun.data.repository.PoemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: PoemRepository,
) : ViewModel() {

    fun observePoem(id: Long): Flow<PoemEntity?> = repository.observePoem(id)
}
