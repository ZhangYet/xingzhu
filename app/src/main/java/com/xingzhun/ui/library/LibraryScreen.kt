package com.xingzhun.ui.library

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xingzhun.data.local.PoemEntity
import com.xingzhun.ui.theme.InkSecondary
import com.xingzhun.ui.theme.Paper
import com.xingzhun.ui.theme.SealBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAddClick: () -> Unit,
    onPoemClick: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val poems by viewModel.poems.collectAsState()

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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(poems, key = { it.id }) { poem ->
            PoemCard(poem = poem, onClick = { onPoemClick(poem.id) })
        }
    }
}

@Composable
private fun PoemCard(poem: PoemEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
