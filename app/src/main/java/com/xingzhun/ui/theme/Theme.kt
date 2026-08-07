package com.xingzhun.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = SealBrown,
    onPrimary = Paper,
    secondary = RhymeRed,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = CardTea,
    onSurfaceVariant = InkSecondary,
    outline = ToneMark,
)

// 正文使用衬线字体，贴合古籍质感（M5 再内置思源宋体，此处先用系统 serif）
private val XingZhunTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 32.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp, lineHeight = 28.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 12.sp),
)

@Composable
fun XingZhunTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // v1 以浅色宣纸风为主，深色主题暂未实现
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = XingZhunTypography,
        content = content,
    )
}
