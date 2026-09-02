package com.xingzhu.ui.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xingzhu.engine.IssueType
import com.xingzhu.engine.TextSplitter
import com.xingzhu.ui.reader.AnnotatedPoemBody
import com.xingzhu.ui.reader.MarkStyle
import com.xingzhu.ui.theme.Ink
import com.xingzhu.ui.theme.InkSecondary
import com.xingzhu.ui.theme.Paper
import com.xingzhu.ui.theme.RhymeRed
import com.xingzhu.ui.theme.SealBrown
import com.xingzhu.ui.theme.ToneLevel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoemCheckScreen(
    onBack: () -> Unit,
    viewModel: PoemCheckViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("习作格律检测", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = TextSplitter.cleanInput(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                placeholder = { Text("粘贴你的诗作（支持五绝/七绝/五律/七律，请用 ，。 断句）") },
                minLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SealBrown,
                    unfocusedBorderColor = InkSecondary,
                ),
            )
            Button(
                onClick = {
                    if (input.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("请先输入诗作") }
                    } else {
                        viewModel.analyze(input)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SealBrown, contentColor = Paper),
            ) {
                Text("分析")
            }

            val annotated = uiState.annotated
            if (annotated != null) {
                Text(
                    text = uiState.form?.let { "体裁：$it" } ?: "该作品非近体诗，仅展示平仄与韵脚",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                val issueLines = uiState.issues
                    .flatMap { issue ->
                        if (issue.type == IssueType.MISMATCH_IN_COUPLET ||
                            issue.type == IssueType.MISMATCH_BETWEEN_COUPLETS
                        ) {
                            listOf(issue.lineIndex, issue.lineIndex + 1)
                        } else {
                            listOf(issue.lineIndex)
                        }
                    }
                    .filter { it in annotated.lines.indices }
                    .toSet()
                AnnotatedPoemBody(
                    annotated = annotated,
                    showTone = true,
                    showRhyme = true,
                    markStyle = MarkStyle.SYMBOL,
                    fontSize = 20f,
                    issueLines = issueLines,
                    modifier = Modifier.padding(top = 12.dp),
                )

                if (uiState.form != null) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (uiState.issues.isEmpty()) {
                            Text(
                                text = "✓ 未发现格律问题",
                                style = MaterialTheme.typography.titleMedium,
                                color = ToneLevel,
                            )
                        } else {
                            Text(
                                text = "格律提示",
                                style = MaterialTheme.typography.titleMedium,
                                color = RhymeRed,
                            )
                            uiState.issues.forEach { issue ->
                                Text(
                                    text = "• ${issue.message}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Ink,
                                )
                            }
                        }
                        if (uiState.unknownCount > 0) {
                            Text(
                                text = "另有 ${uiState.unknownCount} 字无法判定平仄（待考，未作判断）",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSecondary,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
