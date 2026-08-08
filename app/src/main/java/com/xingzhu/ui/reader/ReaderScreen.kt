package com.xingzhu.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xingzhu.engine.TextSplitter
import com.xingzhu.ui.theme.Ink
import com.xingzhu.ui.theme.InkSecondary
import com.xingzhu.ui.theme.Paper
import com.xingzhu.ui.theme.RhymeRed
import com.xingzhu.ui.theme.SealBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    poemId: Long,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val readerPoem by produceState<ReaderPoem>(initialValue = ReaderPoem(null, null), key1 = poemId) {
        viewModel.observe(poemId).collect { value = it }
    }
    var showSettings by remember { mutableStateOf(false) }
    var showTone by rememberSaveable { mutableStateOf(true) }
    var showRhyme by rememberSaveable { mutableStateOf(true) }
    var markStyle by rememberSaveable { mutableStateOf(MarkStyle.SYMBOL) }
    var fontSize by rememberSaveable { mutableStateOf(24f) }

    val poem = readerPoem.entity

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = { Text(poem?.title ?: "", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
            )
        },
    ) { padding ->
        if (poem == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("未找到该诗词", color = InkSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            val annotated = readerPoem.annotated

            val header: @Composable () -> Unit = {
                Text(
                    poem.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    text = listOfNotNull(
                        poem.dynasty,
                        poem.author,
                        poem.form.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                )
            }

            val notes: @Composable () -> Unit = {
                val poemNotes = annotated?.annotations.orEmpty()
                if (showTone || poemNotes.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (showTone) {
                            ToneMarkLegend(markStyle = markStyle, showRhyme = showRhyme)
                        }
                        if (poemNotes.isNotEmpty()) {
                            Text("标注说明", style = MaterialTheme.typography.titleMedium, color = RhymeRed)
                            poemNotes.forEach { note ->
                                Text(note, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                            }
                        }
                    }
                }
            }

            // 横排 / 标注缺失兜底：纵向滚动
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                header()
                if (annotated != null) {
                    AnnotatedPoemBody(
                        annotated = annotated,
                        showTone = showTone,
                        showRhyme = showRhyme,
                        markStyle = markStyle,
                        fontSize = fontSize,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                } else {
                    // 标注缺失时的兜底：纯文本
                    Column(
                        modifier = Modifier.padding(top = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextSplitter.splitSentences(poem.contentText).forEach { line ->
                            Text(
                                text = line,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * 1.8f).sp,
                                letterSpacing = 4.sp,
                                color = Ink,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                notes()
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            SettingsSheet(
                showTone = showTone,
                onShowToneChange = { showTone = it },
                showRhyme = showRhyme,
                onShowRhymeChange = { showRhyme = it },
                markStyle = markStyle,
                onMarkStyleChange = { markStyle = it },
                fontSize = fontSize,
                onFontSizeChange = { fontSize = it },
            )
        }
    }
}

@Composable
private fun SettingsSheet(
    showTone: Boolean,
    onShowToneChange: (Boolean) -> Unit,
    showRhyme: Boolean,
    onShowRhymeChange: (Boolean) -> Unit,
    markStyle: MarkStyle,
    onMarkStyleChange: (MarkStyle) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text("阅读设置", style = MaterialTheme.typography.titleLarge, color = SealBrown)
        SettingSwitch("显示平仄", showTone, onShowToneChange)
        SettingSwitch("显示韵脚", showRhyme, onShowRhymeChange)
        Text("平仄记号样式", style = MaterialTheme.typography.bodyLarge, color = Ink)
        Row(
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MarkStyleChip("〇 ●", MarkStyle.SYMBOL == markStyle) { onMarkStyleChange(MarkStyle.SYMBOL) }
            MarkStyleChip("平 仄", MarkStyle.TEXT == markStyle) { onMarkStyleChange(MarkStyle.TEXT) }
        }
        ToneMarkLegend(markStyle = markStyle, showRhyme = showRhyme)
        Spacer(Modifier.height(12.dp))
        Text("正文字号", style = MaterialTheme.typography.bodyLarge, color = Ink)
        Slider(
            value = fontSize,
            onValueChange = onFontSizeChange,
            valueRange = 18f..34f,
        )
        Text("当前 ${fontSize.toInt()}sp", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
    }
}

@Composable
private fun MarkStyleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
