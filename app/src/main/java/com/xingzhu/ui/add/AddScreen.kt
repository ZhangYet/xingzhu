package com.xingzhu.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xingzhu.ui.theme.InkSecondary
import com.xingzhu.ui.theme.Paper
import com.xingzhu.ui.theme.RhymeRed
import com.xingzhu.ui.theme.SealBrown
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onBack: () -> Unit,
    viewModel: AddViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        containerColor = Paper,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("添加诗词", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
            )
        },
    ) { padding ->
        SearchTab(
            viewModel = viewModel,
            showMessage = showMessage,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

// ── 搜索语料 ───────────────────────────────────────────

@Composable
private fun SearchTab(
    viewModel: AddViewModel,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results by viewModel.results.collectAsState()

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.onQueryChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            placeholder = { Text("按题目、作者或全文搜索（诗经至清诗，共 7 万余首）") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SealBrown,
                unfocusedBorderColor = InkSecondary,
            ),
        )
        if (query.isBlank() && results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "输入题目或作者，从诗经至清诗（7 万余首）中查找",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("没有找到相关诗词", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(results, key = { it.seed.title + "|" + it.seed.author + "|" + it.seed.content.hashCode() }) { item ->
                    SearchResultCard(
                        item = item,
                        query = query,
                        onAdd = {
                            viewModel.add(item.seed)
                            showMessage("已加入书架《${item.seed.title}》")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(item: PoemSearchItem, query: String, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.seed.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = listOfNotNull(item.seed.dynasty, item.seed.author, item.seed.form.takeIf { it.isNotBlank() })
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
                val snippet = matchedSnippet(item.seed.content, query)
                if (snippet != null) {
                    Text(
                        text = highlight(snippet, query.trim()),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (item.inLibrary) {
                Text("已在书架", style = MaterialTheme.typography.labelMedium, color = RhymeRed)
            } else {
                OutlinedButton(
                    onClick = onAdd,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = SealBrown,
                    ),
                ) {
                    Text("加入书架")
                }
            }
        }
    }
}

/** 取正文中首次命中关键词附近的片段 */
private fun matchedSnippet(content: String, query: String): String? {
    val q = query.trim()
    if (q.isEmpty()) return null
    val idx = content.indexOf(q)
    if (idx < 0) return null
    val start = (idx - 10).coerceAtLeast(0)
    val end = (idx + q.length + 10).coerceAtMost(content.length)
    return content.substring(start, end)
}

/** 高亮片段中的关键词 */
private fun highlight(text: String, keyword: String): AnnotatedString = buildAnnotatedString {
    if (keyword.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    var i = 0
    while (i < text.length) {
        val idx = text.indexOf(keyword, i)
        if (idx < 0) {
            append(text.substring(i))
            break
        }
        append(text.substring(i, idx))
        withStyle(SpanStyle(color = RhymeRed, fontWeight = FontWeight.Bold)) {
            append(keyword)
        }
        i = idx + keyword.length
    }
}
