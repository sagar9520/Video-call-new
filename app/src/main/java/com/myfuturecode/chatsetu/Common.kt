package com.myfuturecode.chatsetu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

fun parseColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Purple)

fun hostBrush(h: Host): Brush {
    val c = parseColor(h.color)
    return Brush.linearGradient(listOf(c, Color(0xFF5B2CA0)))
}

/** Host ki photo; photo URL nahi hai to rang + naam ka pehla akshar (demo). */
@Composable
fun HostImage(h: Host, modifier: Modifier, index: Int = 0, letterSize: TextUnit = 40.sp) {
    Box(modifier.background(hostBrush(h)), contentAlignment = Alignment.Center) {
        val url = h.photos.getOrNull(index) ?: h.photos.firstOrNull()
        if (url != null) AsyncImage(url, h.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(h.name.take(1), color = Color.White.copy(alpha = .9f), fontSize = letterSize, fontWeight = FontWeight.Black)
    }
}

@Composable
fun VirtualTag(h: Host, dark: Boolean = true) {
    if (!h.virtual) return
    val label = RemoteConfig.current.virtualLabel
    if (label.isBlank()) return
    Text(
        label, fontSize = 9.sp, fontWeight = FontWeight.Bold,
        color = if (dark) Color.White else LilacText,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (dark) Color.Black.copy(alpha = .5f) else Lilac)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun OnlineDot(online: Boolean) {
    if (!online) return
    Text("● Online", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Green).padding(horizontal = 6.dp, vertical = 2.dp))
}

@Composable
fun GemChip(act: MainActivity, dark: Boolean = false) {
    Text(
        "💎 ${Store.gems.intValue}  ＋", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
        color = if (dark) Color.White else LilacText,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (dark) Color.White.copy(alpha = .18f) else Lilac)
            .clickable { act.recharge = "" }.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, brush: Brush = ButtonGradient, height: Int = 48, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape((height / 2).dp)).background(brush).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f).padding(start = 4.dp))
        actions()
        Spacer(Modifier.width(8.dp))
    }
}

fun openStore(act: MainActivity, pkg: String) {
    val i = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$pkg"))
    runCatching { act.startActivity(i) }.onFailure {
        act.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
    }
}

fun openUrl(act: MainActivity, url: String) {
    runCatching { act.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
}
