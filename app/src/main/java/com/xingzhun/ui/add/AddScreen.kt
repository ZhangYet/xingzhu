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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xingzhun.ui.theme.Ink
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
    var tab by rememberSaveable { mutableIntStateOf(0) }

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
                .padding(padding),
        ) {
            PrimaryTabRow(
                selectedTabIndex = tab,
                containerColor = Paper,
                contentColor = SealBrown,
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("搜索语料") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("手动输入") })
            }
            when (tab) {
                0 -> SearchTab(viewModel)
                else -> ManualTab(viewModel)
            }
        }
    }
}

// ── 搜索语料 ───────────────────────────────────────────

@Composable
private fun SearchTab(viewModel: AddViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    val results by viewModel.results.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
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
            placeholder = { Text("按题目或作者搜索（内置唐诗三百首、宋词三百首）") },
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
                SearchResultCard(item = item, onAdd = { viewModel.add(item.seed) })
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

// ── 手动输入 ───────────────────────────────────────────

@Composable
private fun ManualTab(viewModel: AddViewModel) {
    var title by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }
    var dynasty by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var added by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "输入或粘贴诗词原文，句末字将自动标注为韵脚并给出平仄。",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; added = false },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("题目") },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                modifier = Modifier.weight(1f),
                label = { Text("作者") },
                singleLine = true,
            )
            OutlinedTextField(
                value = dynasty,
                onValueChange = { dynasty = it },
                modifier = Modifier.weight(1f),
                label = { Text("朝代（可选）") },
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = content,
            onValueChange = { content = it; added = false },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("诗词原文") },
        )
        Button(
            onClick = {
                if (viewModel.addManual(title, author, dynasty, "诗", content)) added = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && content.isNotBlank(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = SealBrown,
                contentColor = Paper,
            ),
        ) {
            Text("加入书架")
        }
        if (added) {
            Text("已加入书架", style = MaterialTheme.typography.labelMedium, color = RhymeRed)
        }
    }
}
