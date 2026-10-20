package com.myfuturecode.chatsetu

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple = Color(0xFF9C02BA)
val Indigo = Color(0xFF4A1FC4)
val DeepBlue = Color(0xFF0B1F9E)
val Pink = Color(0xFFFF3B8D)
val Red = Color(0xFFFF3B5C)
val Orange = Color(0xFFFF7A00)
val Gold = Color(0xFFFFB52E)
val Green = Color(0xFF1BB76E)
val Lilac = Color(0xFFF3E8FF)
val LilacText = Color(0xFF7A12B8)
val Night = Color(0xFF140F33)
val SoftBg = Color(0xFFF8F3FD)
val Ink = Color(0xFF1E1B26)

val BrandGradient = Brush.linearGradient(listOf(Purple, Indigo, DeepBlue))
val ButtonGradient = Brush.horizontalGradient(listOf(Purple, Indigo))
val CallGradient = Brush.horizontalGradient(listOf(Pink, Purple))
val GoldGradient = Brush.linearGradient(listOf(Gold, Orange))
val PageGradient = Brush.verticalGradient(listOf(Color.White, Color(0xFFF7ECFF)))

@Composable
fun ChatSetuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, secondary = Indigo, background = SoftBg, surface = Color.White, onSurface = Ink, onBackground = Ink, surfaceContainerLow = Color.White), content = content)
}
