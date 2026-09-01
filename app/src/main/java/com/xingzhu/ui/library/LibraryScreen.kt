package com.xingzhu.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xingzhu.data.local.PoemEntity
import com.xingzhu.ui.theme.CardTea
import com.xingzhu.ui.theme.InkSecondary
import com.xingzhu.ui.theme.Paper
import com.xingzhu.ui.theme.RhymeRed
import com.xingzhu.ui.theme.SealBrown

private sealed interface LibraryItem {
    data class Header(val author: String, val count: Int) : LibraryItem
    data class Poem(val poem: PoemEntity) : LibraryItem
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onAddClick: () -> Unit,
    onCheckClick: () -> Unit,
    onPoemClick: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingDelete by remember { mutableStateOf<PoemEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var collapsedAuthors by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = { Text("行箸", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    Box {
                        TextButton(onClick = { showSortMenu = true }) {
                            Text("排序", style = MaterialTheme.typography.bodyMedium, color = SealBrown)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label) },
                                    trailingIcon = {
                                        if (uiState.sortOrder == order) {
                                            Text("✓", color = SealBrown)
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSortOrder(order)
                                        showSortMenu = false
                                    },
                                )
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("按作者分组", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = uiState.groupByAuthor,
                                    onCheckedChange = { viewModel.setGroupByAuthor(it) },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = onCheckClick,
                    containerColor = CardTea,
                    contentColor = SealBrown,
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "习作格律检测")
                }
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = SealBrown,
                    contentColor = Paper,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "添加诗词")
                }
            }
        },
    ) { padding ->
        if (uiState.poems.isEmpty()) {
            EmptyLibrary(Modifier.padding(padding))
        } else {
            val items = buildItems(
                poems = uiState.poems,
                groupByAuthor = uiState.groupByAuthor,
                collapsedAuthors = collapsedAuthors,
            )
            PoemList(
                items = items,
                onPoemClick = onPoemClick,
                onPoemLongClick = { pendingDelete = it },
                onToggleAuthor = { author ->
                    collapsedAuthors = if (author in collapsedAuthors) {
                        collapsedAuthors - author
                    } else {
                        collapsedAuthors + author
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }

    pendingDelete?.let { poem ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除诗词") },
            text = { Text("确定从书架移除《${poem.title}》吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(poem.id)
                        pendingDelete = null
                    },
                ) {
                    Text("删除", color = RhymeRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

private fun buildItems(
    poems: List<PoemEntity>,
    groupByAuthor: Boolean,
    collapsedAuthors: Set<String>,
): List<LibraryItem> {
    if (!groupByAuthor) return poems.map { LibraryItem.Poem(it) }
    val items = mutableListOf<LibraryItem>()
    poems.groupBy { it.author }.forEach { (author, group) ->
        items.add(LibraryItem.Header(author, group.size))
        if (author !in collapsedAuthors) {
            items.addAll(group.map { LibraryItem.Poem(it) })
        }
    }
    return items
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "书架空空如也\n去诗海中添一首吧",
            style = MaterialTheme.typography.bodyLarge,
            color = InkSecondary,
        )
    }
}

@Composable
private fun PoemList(
    items: List<LibraryItem>,
    onPoemClick: (Long) -> Unit,
    onPoemLongClick: (PoemEntity) -> Unit,
    onToggleAuthor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
    ) {
        items.forEach { item ->
            when (item) {
                is LibraryItem.Header -> item(key = "hdr-${item.author}") {
                    AuthorHeader(
                        author = item.author,
                        count = item.count,
                        onClick = { onToggleAuthor(item.author) },
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                is LibraryItem.Poem -> item(key = "poem-${item.poem.id}") {
                    PoemCard(
                        poem = item.poem,
                        onClick = { onPoemClick(item.poem.id) },
                        onLongClick = { onPoemLongClick(item.poem) },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthorHeader(author: String, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$author（$count 首）",
            style = MaterialTheme.typography.titleSmall,
            color = SealBrown,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PoemCard(
    poem: PoemEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(poem.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = listOfNotNull(poem.dynasty, poem.author, poem.form.takeIf { it.isNotBlank() })
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
            )
        }
    }
}
