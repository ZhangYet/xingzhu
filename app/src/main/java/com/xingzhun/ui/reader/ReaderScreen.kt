package com.xingzhun.ui.reader

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.xingzhun.data.local.PoemEntity
import com.xingzhun.engine.TextSplitter
import com.xingzhun.ui.theme.Ink
import com.xingzhun.ui.theme.InkSecondary
import com.xingzhun.ui.theme.Paper
import com.xingzhun.ui.theme.SealBrown

private enum class LayoutMode { HORIZONTAL, VERTICAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    poemId: Long,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val poem by produceState<PoemEntity?>(initialValue = null, key1 = poemId) {
        viewModel.observePoem(poemId).collect { value = it }
    }
    var layout by rememberSaveable { mutableStateOf(LayoutMode.HORIZONTAL) }
    var showSettings by remember { mutableStateOf(false) }
    var showTone by rememberSaveable { mutableStateOf(true) }
    var showRhyme by rememberSaveable { mutableStateOf(true) }
    var fontSize by rememberSaveable { mutableStateOf(24f) }

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
                    TextButton(onClick = { layout = if (layout == LayoutMode.HORIZONTAL) LayoutMode.VERTICAL else LayoutMode.HORIZONTAL }) {
                        Text(
                            text = if (layout == LayoutMode.HORIZONTAL) "竖排" else "横排",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SealBrown,
                        )
                    }
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
            PoemBody(
                poem = poem!!,
                layout = layout,
                showTone = showTone,
                showRhyme = showRhyme,
                fontSize = fontSize,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            SettingsSheet(
                showTone = showTone,
                onShowToneChange = { showTone = it },
                showRhyme = showRhyme,
                onShowRhymeChange = { showRhyme = it },
                fontSize = fontSize,
                onFontSizeChange = { fontSize = it },
            )
        }
    }
}

@Composable
private fun PoemBody(
    poem: PoemEntity,
    layout: LayoutMode,
    showTone: Boolean,
    showRhyme: Boolean,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Text(
            poem.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Ink,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = listOfNotNull(poem.dynasty, poem.author, poem.form).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
        )
        Box(Modifier.padding(top = 24.dp)) {
            val lines = TextSplitter.splitSentences(poem.contentText)
            if (layout == LayoutMode.HORIZONTAL) {
                HorizontalPoem(lines, fontSize)
            } else {
                VerticalPoem(lines, fontSize)
            }
        }
        // M3 起：showTone / showRhyme 控制平仄记号与韵脚渲染
    }
}

@Composable
private fun HorizontalPoem(lines: List<String>, fontSize: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        lines.forEach { line ->
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

@Composable
private fun VerticalPoem(lines: List<String>, fontSize: Float) {
    // 竖排：列自右向左，每列自上而下
    Row(
        modifier = Modifier
            .heightIn(min = 320.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.Top,
    ) {
        lines.asReversed().forEach { line ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                line.forEach { char ->
                    Text(char.toString(), fontSize = fontSize.sp, color = Ink)
                }
            }
        }
    }
}

@Composable
private fun SettingsSheet(
    showTone: Boolean,
    onShowToneChange: (Boolean) -> Unit,
    showRhyme: Boolean,
    onShowRhymeChange: (Boolean) -> Unit,
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
        Text("正文字号（M3 接入字号滑块）", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        Text("当前字号 ${fontSize.toInt()}sp", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
    }
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
