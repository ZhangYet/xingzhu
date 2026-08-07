package com.xingzhun.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xingzhun.data.local.PoemEntity
import com.xingzhun.ui.theme.InkSecondary
import com.xingzhun.ui.theme.Paper
import com.xingzhun.ui.theme.RhymeRed
import com.xingzhun.ui.theme.SealBrown

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onAddClick: () -> Unit,
    onPoemClick: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val poems by viewModel.poems.collectAsState()
    var pendingDelete by remember { mutableStateOf<PoemEntity?>(null) }

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = { Text("行箸", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = SealBrown,
                contentColor = Paper,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加诗词")
            }
        },
    ) { padding ->
        if (poems.isEmpty()) {
            EmptyLibrary(Modifier.padding(padding))
        } else {
            PoemList(
                poems = poems,
                onPoemClick = onPoemClick,
                onPoemLongClick = { pendingDelete = it },
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
    poems: List<PoemEntity>,
    onPoemClick: (Long) -> Unit,
    onPoemLongClick: (PoemEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(poems, key = { it.id }) { poem ->
            PoemCard(
                poem = poem,
                onClick = { onPoemClick(poem.id) },
                onLongClick = { onPoemLongClick(poem) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PoemCard(
    poem: PoemEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(poem.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = listOfNotNull(poem.dynasty, poem.author).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
            )
        }
    }
}
