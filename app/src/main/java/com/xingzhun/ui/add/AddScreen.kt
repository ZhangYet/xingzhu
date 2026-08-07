package com.xingzhun.ui.add

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xingzhun.ui.theme.InkSecondary
import com.xingzhun.ui.theme.Paper
import com.xingzhun.ui.theme.RhymeRed
import com.xingzhun.ui.theme.SealBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onBack: () -> Unit,
    viewModel: AddViewModel = hiltViewModel(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results by viewModel.results.collectAsState()

    Scaffold(
        containerColor = Paper,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.onQueryChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("按题目或作者搜索") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SealBrown,
                    unfocusedBorderColor = InkSecondary,
                ),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(results, key = { it.seed.title + it.seed.author }) { item ->
                    SearchResultCard(
                        item = item,
                        onAdd = { viewModel.add(item.seed) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(item: PoemSearchItem, onAdd: () -> Unit) {
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
                    text = listOf(item.seed.dynasty, item.seed.author, item.seed.form)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
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
