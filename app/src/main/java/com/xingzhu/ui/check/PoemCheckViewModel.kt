package com.xingzhu.ui.check

import androidx.lifecycle.ViewModel
import com.xingzhu.engine.AnnotatedPoem
import com.xingzhu.engine.PingZeChecker
import com.xingzhu.engine.PingZeEngine
import com.xingzhu.engine.PingZeIssue
import com.xingzhu.engine.ToneClass
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PoemCheckUiState(
    val annotated: AnnotatedPoem? = null,
    /** 自动识别的近体诗体裁；null = 非近体诗 */
    val form: String? = null,
    val issues: List<PingZeIssue> = emptyList(),
    /** 待考（无法判定平仄）的汉字数 */
    val unknownCount: Int = 0,
    val analyzed: Boolean = false,
)

@HiltViewModel
class PoemCheckViewModel @Inject constructor(
    private val engine: PingZeEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PoemCheckUiState())
    val uiState: StateFlow<PoemCheckUiState> = _uiState.asStateFlow()

    fun analyze(content: String) {
        val text = content.trim()
        if (text.isEmpty()) {
            _uiState.value = PoemCheckUiState()
            return
        }
        val form = PingZeChecker.detectForm(text)
        val annotated = engine.annotatePoem(poemId = 0L, form = form ?: "古体", contentText = text)
        val issues = if (form != null) PingZeChecker.check(annotated, form) else emptyList()
        val unknownCount = annotated.lines.sumOf { line ->
            line.chars.count { it.tone == ToneClass.UNKNOWN && it.char in '\u4E00'..'\u9FFF' }
        }
        _uiState.value = PoemCheckUiState(
            annotated = annotated,
            form = form,
            issues = issues,
            unknownCount = unknownCount,
            analyzed = true,
        )
    }
}
