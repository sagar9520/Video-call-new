package com.myfuturecode.chatsetu

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.Close
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------- Profile ----------------

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(act: MainActivity, hostId: String) {
    val h = act.host(hostId) ?: run { act.back(); return }
    var report by remember { mutableStateOf(false) }
    val pages = (h.photos.size + if (h.video.isNotBlank()) 1 else 0).coerceAtLeast(1)
    val pager = rememberPagerState { pages }
    DarkStatusBar()
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(380.dp)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { p ->
                if (h.video.isNotBlank() && p == 0) LoopVideo(h.video, Modifier.fillMaxSize(), muted = true)
                else HostImage(h, Modifier.fillMaxSize(), index = p - if (h.video.isNotBlank()) 1 else 0, letterSize = 110.sp)
            }
            Row(Modifier.statusBarsPadding().fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ act.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Spacer(Modifier.weight(1f))
                VirtualTag(h)
                IconButton({ report = true }) { Icon(Icons.Default.Flag, "Report", tint = Color.White) }
            }
            if (pages > 1) Row(Modifier.align(Alignment.BottomCenter).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(pages) { i -> Box(Modifier.size(width = if (i == pager.currentPage) 18.dp else 6.dp, height = 4.dp).clip(CircleShape).background(Color.White.copy(alpha = if (i == pager.currentPage) 1f else .5f))) }
            }
        }
        Column(Modifier.weight(1f).padding(16.dp)) {
            val following = h.id in Store.following
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${h.name}, ${h.age}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Spacer(Modifier.width(8.dp))
                        if (h.online) Text("● Online", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("🇮🇳 ${h.city} · ${h.lang} · ID ${h.id}", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                    Text("${h.followers + if (following) 1 else 0} followers", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(if (following) SolidColor(Lilac) else ButtonGradient)
                        .clickable { if (Store.toggleFollow(h.id)) act.toast("You are following ${h.name}") }.padding(horizontal = 16.dp, vertical = 8.dp),
                ) { Text(if (following) "Following" else "+ Follow", color = if (following) LilacText else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
            if (h.bio.isNotBlank()) Text(h.bio, fontSize = 14.sp, color = Color(0xFF444444), modifier = Modifier.padding(top = 10.dp))
            FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                h.tags.forEach { Text(it, fontSize = 11.sp, color = LilacText, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Lilac).padding(horizontal = 9.dp, vertical = 4.dp)) }
            }
            if (h.virtual) Text(
                "This is a demo/virtual profile. Photos, videos and messages are sample content.",
                fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 12.dp),
            )
        }
        Row(Modifier.navigationBarsPadding().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Lilac).clickable { act.push(Route.Chat(h.id)) },
                contentAlignment = Alignment.Center,
            ) { Text("💬 Chat", color = LilacText, fontWeight = FontWeight.Bold) }
            Box(
                Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFE8F8F0)).clickable { act.startCall(h.id, audio = true) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Call, "Voice call", tint = Green) }
            GradientButton("📹 Video call · ${h.rate}💎/min", Modifier.weight(1.5f), brush = CallGradient, height = 50) { act.startCall(h.id) }
        }
    }
    if (report) ReportDialog(act, h) { report = false }
}

@Composable
fun ReportDialog(act: MainActivity, h: Host, close: () -> Unit) {
    val reasons = listOf("Fake profile", "Nudity / sexual content", "Harassment", "Spam or scam", "Underage", "Other")
    var pick by remember { mutableStateOf(reasons[0]) }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Report ${h.name}") },
        text = {
            Column {
                reasons.forEach { r ->
                    Row(Modifier.fillMaxWidth().clickable { pick = r }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(pick == r, { pick = r }); Text(r)
                    }
                }
                Text("The profile will also be blocked for you.", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton({
                Store.block(h.id); close(); act.toast("Thanks, we'll review this report")
                act.reset(Route.Main())
            }) { Text("Report & block", color = Red) }
        },
        dismissButton = { TextButton(close) { Text("Cancel") } },
    )
}

// ---------------- Chat ----------------

@Composable
fun ChatScreen(act: MainActivity, hostId: String) {
    val h = act.host(hostId) ?: run { act.back(); return }
    val cfg = RemoteConfig.current
    val script = h.script ?: cfg.chatScript
    val msgs = Store.chats[h.id] ?: emptyList()
    var text by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var pendingReplies by remember { mutableIntStateOf(0) }
    var gifts by remember { mutableStateOf(false) }
    var viewer by remember { mutableStateOf<ChatMsg?>(null) }
    val list = rememberLazyListState()

    LaunchedEffect(msgs.size) { Store.markRead(h.id); if (msgs.isNotEmpty()) list.animateScrollToItem(msgs.lastIndex) }

    // Pehli baar chat khule to greeting; har user message ke baad script ka agla message
    LaunchedEffect(h.id) { if (msgs.isEmpty()) pendingReplies = 1 }
    LaunchedEffect(pendingReplies) {
        if (pendingReplies <= 0) return@LaunchedEffect
        val pos = Store.scriptPos[h.id] ?: 0
        val next = script.getOrNull(pos)
        val reply = next ?: ScriptMsg("text", "Let's continue on a video call 📹", "", 0, 3)
        if (next == null && Store.chats[h.id]?.lastOrNull()?.text == reply.text) { pendingReplies = 0; return@LaunchedEffect }
        delay(900)
        typing = true
        delay((reply.delay.coerceIn(1, 6)) * 700L)
        typing = false
        Store.addMsg(h.id, ChatMsg(false, reply.type, reply.text, reply.url, reply.duration, System.currentTimeMillis()), countUnread = false)
        if (next != null) Store.advanceScript(h.id)
        pendingReplies--
    }

    fun send() {
        val t = text.trim()
        if (t.isEmpty()) return
        if (!Store.spend(cfg.msgCost)) { act.needGems(); return }
        Store.addMsg(h.id, ChatMsg(true, "text", t, "", 0, System.currentTimeMillis()), countUnread = false)
        text = ""
        pendingReplies++
    }

    Column(Modifier.fillMaxSize().background(SoftBg).imePadding()) {
        Row(Modifier.fillMaxWidth().background(Color.White).statusBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ act.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            HostImage(h, Modifier.size(38.dp).clip(CircleShape).clickable { act.push(Route.Profile(h.id)) }, letterSize = 16.sp)
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(h.name, fontWeight = FontWeight.Bold); Spacer(Modifier.width(6.dp)); VirtualTag(h, dark = false) }
                Text(if (typing) "typing…" else if (h.online) "● Online" else "Offline", fontSize = 11.sp, color = if (h.online || typing) Green else Color.Gray)
            }
            IconButton({ act.startCall(h.id, audio = true) }) { Icon(Icons.Default.Call, "Voice call", tint = Green) }
            IconButton({ act.startCall(h.id) }) { Icon(Icons.Default.Videocam, "Video call", tint = Purple) }
        }
        LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(vertical = 10.dp)) {
            items(msgs) { m -> Bubble(m) { viewer = m } }
            if (typing) item { Text("…", fontSize = 22.sp, modifier = Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(horizontal = 14.dp)) }
        }
        Row(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ gifts = true }) { Icon(Icons.Default.CardGiftcard, "Gift", tint = Pink) }
            TextField(
                text, { text = it }, Modifier.weight(1f), placeholder = { Text("Type a message… (${cfg.msgCost}💎)", fontSize = 13.sp) },
                shape = RoundedCornerShape(22.dp), maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF3F0F8), unfocusedContainerColor = Color(0xFFF3F0F8),
                ),
            )
            IconButton({ send() }) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(ButtonGradient), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
    if (gifts) GiftSheet(act, h, onClose = { gifts = false }) { g ->
        Store.addMsg(h.id, ChatMsg(true, "gift", "${g.emoji} ${g.name}", "", 0, System.currentTimeMillis()), countUnread = false)
        pendingReplies++
    }
    viewer?.let { m -> MediaViewer(m) { viewer = null } }
}

@Composable
private fun Bubble(m: ChatMsg, open: () -> Unit) {
    val mine = m.mine
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (mine) 16.dp else 4.dp, bottomEnd = if (mine) 4.dp else 16.dp)
        val bg = if (mine) Modifier.background(ButtonGradient, shape) else Modifier.background(Color.White, shape).border(1.dp, Color(0xFFEEEEEE), shape)
        when (m.type) {
            "image" -> AsyncImage(m.url, "Photo", Modifier.width(180.dp).height(220.dp).clip(RoundedCornerShape(14.dp)).background(Lilac).clickable(onClick = open), contentScale = ContentScale.Crop)
            "video" -> Box(Modifier.width(180.dp).height(130.dp).clip(RoundedCornerShape(14.dp)).background(BrandGradient).clickable(onClick = open), contentAlignment = Alignment.Center) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = .4f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, "Play", tint = Color.White) }
                Text("🎬 Video", color = Color.White, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
            }
            "voice" -> VoiceBubble(m, Modifier.clip(shape).then(bg))
            "gift" -> Text(m.text, fontSize = 22.sp, modifier = Modifier.clip(shape).background(Lilac).padding(horizontal = 14.dp, vertical = 8.dp))
            else -> Text(m.text, color = if (mine) Color.White else Color(0xFF222222), fontSize = 14.sp, modifier = Modifier.widthIn(max = 260.dp).then(bg).padding(horizontal = 12.dp, vertical = 9.dp))
        }
    }
}

@Composable
private fun VoiceBubble(m: ChatMsg, modifier: Modifier) {
    val ctx = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    val player = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(Unit) {
        val l = object : Player.Listener { override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying } }
        player.addListener(l)
        onDispose { player.release() }
    }
    Row(modifier.width(200.dp).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Purple).clickable {
            if (playing) player.pause() else {
                if (player.mediaItemCount == 0) { player.setMediaItem(MediaItem.fromUri(m.url)); player.prepare() }
                player.play()
            }
        }, contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Row(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(6, 12, 8, 16, 10, 14, 6, 12, 9, 15, 7, 11).forEach { hgt ->
                Box(Modifier.width(3.dp).height(hgt.dp).clip(CircleShape).background(Purple.copy(alpha = if (playing) 1f else .5f)))
            }
        }
        Text("0:${m.duration.toString().padStart(2, '0')}", fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun MediaViewer(m: ChatMsg, close: () -> Unit) {
    Dialog(close, DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).clickable(onClick = close), contentAlignment = Alignment.Center) {
            if (m.type == "video") LoopVideo(m.url, Modifier.fillMaxWidth().aspectRatio(9f / 16f), muted = false, fit = true)
            else AsyncImage(m.url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

/** Server URL wala video, loop me, bina controls. */
@Composable
fun LoopVideo(url: String, modifier: Modifier, muted: Boolean, fit: Boolean = false) {
    val ctx = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(url)); repeatMode = Player.REPEAT_MODE_ALL
            volume = if (muted) 0f else 1f; playWhenReady = true; prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(modifier = modifier.background(Color.Black), factory = {
        PlayerView(it).apply {
            useController = false
            resizeMode = if (fit) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            this.player = player
        }
    })
}

// ---------------- Video call ----------------

@Composable
fun CallScreen(act: MainActivity, hostId: String, fromMatch: Boolean, audio: Boolean = false) {
    val h = act.host(hostId) ?: run { act.back(); return }
    val ctx = LocalContext.current
    val rate = if (audio) h.audioRate else h.rate
    val kind = if (audio) "voice" else "video"
    var speaker by remember { mutableStateOf(true) }
    var connecting by remember { mutableStateOf(true) }
    var seconds by remember { mutableIntStateOf(0) }
    var micOn by remember { mutableStateOf(true) }
    var camOn by remember { mutableStateOf(true) }
    var front by remember { mutableStateOf(true) }
    var gifts by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf(false) }
    var lastGift by remember { mutableStateOf<String?>(null) }
    var bigGift by remember { mutableStateOf<String?>(null) }
    val callMsgs = remember { mutableStateListOf<Pair<Boolean, String>>() }   // (mine, text)
    var chatOpen by remember { mutableStateOf(false) }
    var chatText by remember { mutableStateOf("") }
    var camAllowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val askCam = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { camAllowed = it }

    fun end() {
        act.back()
        if (seconds > 5) act.toast("Call ended · %d:%02d".format(seconds / 60, seconds % 60))
        Ads.maybeInterstitial(act)
    }

    LaunchedEffect(Unit) {
        if (!audio && !camAllowed) askCam.launch(Manifest.permission.CAMERA)
        // Pehla minute shuru me hi katega
        if (!Store.spend(rate)) { act.back(); act.recharge = "Recharge to start a $kind call with ${h.name}"; return@LaunchedEffect }
        delay(if (fromMatch) 1200 else 2200)
        connecting = false
        while (true) {
            delay(1000)
            seconds++
            if (seconds % 60 == 0 && !Store.spend(rate)) {
                act.back(); act.recharge = "Recharge to continue the call with ${h.name}"; break
            }
        }
    }
    LaunchedEffect(lastGift) { if (lastGift != null) { delay(2500); lastGift = null } }
    LaunchedEffect(bigGift) { if (bigGift != null) { delay(1800); bigGift = null } }
    // Call ke dauraan host ke text messages (server script se) screen pe aate hain
    LaunchedEffect(connecting) {
        if (connecting) return@LaunchedEffect
        val lines = (h.script ?: RemoteConfig.current.chatScript).filter { it.type == "text" }.map { it.text }
        lines.forEach { t -> delay(6000L + (0..4000).random()); callMsgs.add(false to t) }
    }

    DarkStatusBar()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (audio) {
            // Voice call: badi photo dhundhli + beech me avatar; awaz host ke clip se (demo)
            HostImage(h, Modifier.fillMaxSize().blur(28.dp), index = 1, letterSize = 120.sp)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)))
            if (!connecting && h.video.isNotBlank() && speaker) LoopVideo(h.video, Modifier.size(1.dp), muted = false)
            val pulse = rememberInfiniteTransition(label = "v").animateFloat(1f, 1.25f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p")
            Column(Modifier.align(Alignment.Center).padding(bottom = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.size(150.dp).scale(if (connecting) 1f else pulse.value).clip(CircleShape).background(Color.White.copy(alpha = .15f)))
                    HostImage(h, Modifier.size(130.dp).clip(CircleShape).border(3.dp, Color.White, CircleShape), letterSize = 50.sp)
                }
                Text(h.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
                Text(if (connecting) "Calling…" else "Voice call · %d:%02d".format(seconds / 60, seconds % 60), color = Color.White.copy(alpha = .85f), modifier = Modifier.padding(top = 4.dp))
            }
        } else if (h.video.isNotBlank()) LoopVideo(h.video, Modifier.fillMaxSize(), muted = false)
        else HostImage(h, Modifier.fillMaxSize(), letterSize = 120.sp)

        if (connecting && !audio) Box(Modifier.fillMaxSize().background(hostBrush(h)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HostImage(h, Modifier.size(110.dp).clip(CircleShape).border(3.dp, Color.White, CircleShape), letterSize = 44.sp)
                Text(h.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
                Text(if (fromMatch) "Matched! Connecting…" else "Calling…", color = Color.White.copy(alpha = .85f), modifier = Modifier.padding(top = 4.dp))
                Text("${if (audio) "Voice call" else "Video call"} · $rate 💎 / min", color = Color.White.copy(alpha = .7f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }

        // Upar: host info, timer, report
        Row(Modifier.statusBarsPadding().fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            HostImage(h, Modifier.size(38.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape), letterSize = 16.sp)
            Column(Modifier.padding(start = 8.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(h.name, color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.width(6.dp)); VirtualTag(h) }
                Text("💎 ${Store.gems.intValue} · -$rate/min", color = Color.White.copy(alpha = .85f), fontSize = 11.sp)
            }
            if (!connecting) Text("%d:%02d".format(seconds / 60, seconds % 60), color = Color.White, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .4f)).padding(horizontal = 10.dp, vertical = 4.dp))
            Spacer(Modifier.width(6.dp))
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Red).clickable { report = true }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("⚑ Report", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Apna camera (chhota box)
        if (!audio) Box(
            Modifier.statusBarsPadding().padding(top = 64.dp, end = 12.dp).align(Alignment.TopEnd).size(width = 100.dp, height = 140.dp)
                .clip(RoundedCornerShape(14.dp)).background(Color(0xFF2B2357)).border(2.dp, Color.White.copy(alpha = .5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (camAllowed && camOn) CameraPreview(front, Modifier.fillMaxSize())
            else Text(if (!camAllowed) "Camera\npermission\nneeded" else "Camera off", color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
        }

        // Call chat (neeche-baayen)
        Column(
            Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 12.dp, end = 90.dp, bottom = if (chatOpen) 150.dp else 100.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            callMsgs.takeLast(4).forEach { (mine, t) ->
                Text(
                    (if (mine) "You: " else "${h.name}: ") + t, color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .45f)).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        bigGift?.let { Text(it, fontSize = 110.sp, modifier = Modifier.align(Alignment.Center)) }
        if (chatOpen) Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 92.dp).fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)).background(Color.White).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (chatText.isEmpty()) Text("Say something… (${RemoteConfig.current.msgCost}💎)", color = Color.Gray, fontSize = 14.sp)
                androidx.compose.foundation.text.BasicTextField(chatText, { chatText = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Ink))
            }
            IconButton({
                val t = chatText.trim()
                if (t.isNotEmpty()) {
                    if (!Store.spend(RemoteConfig.current.msgCost)) act.needGems()
                    else { callMsgs.add(true to t); chatText = "" }
                }
            }) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Purple) }
        }

        lastGift?.let {
            Text(it, color = Color.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 12.dp, bottom = 100.dp)
                .clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .5f)).padding(horizontal = 12.dp, vertical = 6.dp))
        }

        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().fillMaxWidth().padding(bottom = 22.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
        ) {
            CallBtn(if (micOn) Icons.Default.Mic else Icons.Default.MicOff, "Mic") { micOn = !micOn }
            if (audio) CallBtn(if (speaker) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, "Speaker") { speaker = !speaker }
            else CallBtn(if (camOn) Icons.Default.Videocam else Icons.Default.VideocamOff, "Camera") { camOn = !camOn }
            CallBtn(Icons.Default.CallEnd, "End", size = 62, bg = Brush.linearGradient(listOf(Red, Red))) { end() }
            if (!audio) CallBtn(Icons.Default.Cameraswitch, "Flip") { front = !front }
            CallBtn(Icons.Default.CardGiftcard, "Gift") { gifts = true }
            CallBtn(Icons.AutoMirrored.Filled.Chat, "Chat") { chatOpen = !chatOpen }
        }
    }
    if (gifts) GiftSheet(act, h, onClose = { gifts = false }) { g -> lastGift = "🎁 You sent ${g.emoji} ${g.name}"; bigGift = g.emoji }
    if (report) ReportDialog(act, h) { report = false }
}

@Composable
private fun CallBtn(icon: ImageVector, desc: String, size: Int = 48, bg: Brush = Brush.linearGradient(listOf(Color.White.copy(alpha = .22f), Color.White.copy(alpha = .22f))), onClick: () -> Unit) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = Color.White)
    }
}

@Composable
private fun CameraPreview(front: Boolean, modifier: Modifier) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    DisposableEffect(front) {
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            runCatching {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                provider.unbindAll()
                provider.bindToLifecycle(owner, if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA, preview)
            }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose { runCatching { future.get().unbindAll() } }
    }
    AndroidView({ view }, modifier)
}

// ---------------- Gifts ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftSheet(act: MainActivity, h: Host, onClose: () -> Unit, onSent: (Gift) -> Unit) {
    val gifts = RemoteConfig.current.gifts
    val cats = gifts.map { it.category }.distinct()
    var cat by remember { mutableStateOf(cats.firstOrNull() ?: "") }
    var sel by remember { mutableStateOf<Gift?>(null) }
    var qty by remember { mutableIntStateOf(1) }
    // ModalBottomSheet khud nav bar ke upar rehta hai (button ab neeche nahi chhupega)
    ModalBottomSheet(onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color(0xFF1A1030), dragHandle = null) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                cats.forEach { c ->
                    Column(Modifier.clickable { cat = c }.padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(c, color = if (c == cat) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(Modifier.padding(top = 3.dp).size(width = 14.dp, height = 2.dp).background(if (c == cat) Pink else Color.Transparent))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("💎 ${Store.gems.intValue}  ›", color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onClose(); act.recharge = "" })
            }
            Column(Modifier.padding(vertical = 8.dp)) {
                gifts.filter { it.category == cat }.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { g ->
                            Column(
                                Modifier.weight(1f).padding(3.dp).clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, if (sel == g) Pink else Color.Transparent, RoundedCornerShape(12.dp))
                                    .background(if (sel == g) Pink.copy(alpha = .15f) else Color.Transparent)
                                    .clickable { sel = g }.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (g.icon.isNotBlank()) AsyncImage(g.icon, g.name, Modifier.size(42.dp)) else Text(g.emoji, fontSize = 34.sp)
                                Text(g.name, color = Color(0xFFDDDDDD), fontSize = 11.sp)
                                Text("💎${g.price}", color = Gold, fontSize = 10.sp)
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "x$qty ▾", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .12f))
                        .clickable { qty = when (qty) { 1 -> 5; 5 -> 10; 10 -> 99; else -> 1 } }.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                Spacer(Modifier.width(10.dp))
                GradientButton(sel?.let { "Send · ${it.price * qty}💎" } ?: "Choose a gift", Modifier.weight(1f), brush = CallGradient, height = 46) {
                    val g = sel ?: return@GradientButton
                    if (!Store.spend(g.price * qty)) { onClose(); act.needGems(); return@GradientButton }
                    onSent(g)
                    onClose()
                }
            }
        }
    }
}

// ---------------- Purchases / Blocked ----------------

@Composable
fun PurchasesScreen(act: MainActivity) {
    val scope = rememberCoroutineScope()
    val fmt = remember { java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT) }
    Column(Modifier.fillMaxSize().background(Color.White).navigationBarsPadding()) {
        TopBar("Purchase history", onBack = { act.back() }) {
            if (Store.purchases.any { it.status == "Pending" }) TextButton({ scope.launch { Billing.verifyPending(); act.toast("Status updated") } }) { Text("Check status") }
        }
        if (Store.purchases.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No purchases yet", color = Color.Gray) }
        else LazyColumn {
            items(Store.purchases.toList()) { p ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("💎 ${p.gems} · ${p.price}", fontWeight = FontWeight.Bold)
                        Text("${p.mode} · ${p.orderId}", fontSize = 11.sp, color = Color.Gray)
                        Text(fmt.format(java.util.Date(p.time)), fontSize = 11.sp, color = Color.Gray)
                    }
                    Text(p.status, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = when (p.status) { "Paid" -> Green; "Pending" -> Orange; else -> Red })
                }
                HorizontalDivider(color = Color(0xFFF2F2F2))
            }
        }
    }
}

@Composable
fun BlockedScreen(act: MainActivity) {
    val hosts = RemoteConfig.current.hosts.filter { it.id in Store.blocked }
    Column(Modifier.fillMaxSize().background(Color.White).navigationBarsPadding()) {
        TopBar("Blocked", onBack = { act.back() })
        if (hosts.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No one is blocked", color = Color.Gray) }
        else LazyColumn {
            items(hosts) { h ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    HostImage(h, Modifier.size(44.dp).clip(CircleShape), letterSize = 18.sp)
                    Text(h.name, Modifier.padding(start = 12.dp).weight(1f), fontWeight = FontWeight.SemiBold)
                    TextButton({ Store.unblock(h.id) }) { Text("Unblock") }
                }
            }
        }
    }
}

// ---------------- Global popups: recharge, gems kam, check-in ----------------

@Composable
fun Overlays(act: MainActivity) {
    act.recharge?.let { msg -> RechargeSheet(act, msg) { act.recharge = null } }
    if (act.lowGems) LowGemsDialog(act)
    if (act.checkin) CheckinDialog(act)
    act.banner?.let { (h, text) ->
        Box(Modifier.fillMaxSize().statusBarsPadding().padding(10.dp), contentAlignment = Alignment.TopCenter) {
            Row(
                Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Color.White)
                    .clickable { act.banner = null; act.push(Route.Chat(h.id)) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HostImage(h, Modifier.size(42.dp).clip(CircleShape), letterSize = 16.sp)
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(h.name, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.width(6.dp)); VirtualTag(h, dark = false) }
                    Text(text, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                }
                Text("Reply", color = Purple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
    act.onlineSuggest?.let { h -> OnlineCard(act, h) }
    act.connectingHost?.let { h ->
        Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 90.dp), contentAlignment = Alignment.BottomCenter) {
            Row(
                Modifier.clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = .75f)).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HostImage(h, Modifier.size(26.dp).clip(CircleShape), letterSize = 12.sp)
                Text("  Connecting ${h.name}…", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

/** "X abhi online hai" card: user khud call/chat chune (koi nakli incoming call nahi). */
@Composable
private fun OnlineCard(act: MainActivity, h: Host) {
    Dialog({ act.onlineSuggest = null }) {
        Column(Modifier.width(290.dp).clip(RoundedCornerShape(24.dp)).background(Color.White)) {
            Box(Modifier.fillMaxWidth().height(250.dp)) {
                HostImage(h, Modifier.fillMaxSize(), index = 1, letterSize = 80.sp)
                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) { VirtualTag(h); OnlineDot(true) }
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .7f)))).padding(14.dp)) {
                    Text("${h.name}, ${h.age}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text("is online now · ${h.city}", color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
                }
                IconButton({ act.onlineSuggest = null }, Modifier.align(Alignment.TopEnd).padding(top = 30.dp)) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
            }
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Lilac).clickable { act.onlineSuggest = null; act.push(Route.Chat(h.id)) }, contentAlignment = Alignment.Center) {
                    Text("💬 Chat", color = LilacText, fontWeight = FontWeight.Bold)
                }
                GradientButton("📹 Call", Modifier.weight(1f), brush = CallGradient, height = 46) { act.startCall(h.id) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RechargeSheet(act: MainActivity, msg: String, close: () -> Unit) {
    val cfg = RemoteConfig.current
    var sel by remember { mutableStateOf(cfg.packs.firstOrNull()) }
    ModalBottomSheet(close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color(0xFFFBF5FF), dragHandle = null) {
        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF3E4FF)))).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(BrandGradient), contentAlignment = Alignment.Center) { Text("💎", fontSize = 19.sp) }
                Text(msg.ifBlank { "Recharge gems to call, chat and send gifts" }, fontSize = 13.sp, color = Color(0xFF333333), modifier = Modifier.padding(start = 10.dp).weight(1f))
                IconButton(close) { Icon(Icons.Default.Close, "Close", tint = Color.Gray) }
            }
            Spacer(Modifier.height(10.dp))
            cfg.packs.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { p ->
                        val on = sel == p
                        Box(Modifier.weight(1f)) {
                            Column(
                                Modifier.padding(top = 6.dp).fillMaxWidth().shadow(if (on) 6.dp else 1.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                                    .background(if (on) GoldGradient else SolidColor(Color.White))
                                    .border(1.5.dp, if (on) Orange else Color(0xFFF0E8F7), RoundedCornerShape(14.dp))
                                    .clickable { sel = p }.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(if (p.gems > 50000) "💎💎" else "💎", fontSize = 22.sp)
                                Text("%,d".format(p.gems), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (on) Color.White else Ink, modifier = Modifier.padding(top = 2.dp))
                                Text(
                                    Billing.playPrice(p) ?: p.price, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (on) Orange else Orange,
                                    modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(10.dp)).background(if (on) Color.White else Color.Transparent).padding(horizontal = 10.dp, vertical = 2.dp),
                                )
                            }
                            if (p.off.isNotBlank()) Text(p.off, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp)).background(Color(0xFFFF5A3C)).padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Text("💎 My Gems: ${Store.gems.intValue}", fontSize = 13.sp, color = Color(0xFF555555), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            val p = sel
            val mode = cfg.paymentMode
            if (mode == "play" || mode == "both") GradientButton(if (mode == "both") "Pay with Google Play" else "Continue", height = 50) {
                if (p != null) { close(); Billing.buyPlay(act, p) }
            }
            if (mode == "both") Spacer(Modifier.height(8.dp))
            if (mode == "cashfree" || mode == "both") GradientButton("Pay with UPI / Card", brush = GoldGradient, height = 50) {
                if (p != null) { close(); Billing.buyCashfree(act, p) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LowGemsDialog(act: MainActivity) {
    val cfg = RemoteConfig.current
    Dialog({ act.lowGems = false }) {
        Box(contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.padding(top = 34.dp).width(260.dp).clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(listOf(Color(0xFFFFF0F3), Color.White))).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(26.dp))
                Text("Insufficient gems", color = Color(0xFFE0405A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(14.dp))
                GradientButton("💎 Recharge", brush = Brush.horizontalGradient(listOf(Color(0xFFFF5A3C), Color(0xFFFF3B6B))), height = 44) {
                    act.lowGems = false; act.recharge = ""
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).border(1.5.dp, Color(0xFFFF7A50), RoundedCornerShape(22.dp)).clickable {
                        act.lowGems = false
                        Ads.showRewarded(act, onReward = { Store.addGems(cfg.rewardedGems); act.toast("+${cfg.rewardedGems} gems") }, onFail = { act.toast("Ad not ready, try again") })
                    },
                    contentAlignment = Alignment.Center,
                ) { Text("▶ Get gems for free", color = Color(0xFFFF5A3C), fontWeight = FontWeight.Bold) }
            }
            Text("🥺", fontSize = 56.sp)
        }
    }
}

@Composable
private fun CheckinDialog(act: MainActivity) {
    val rewards = RemoteConfig.current.checkin
    val today = Store.checkinIndex
    var claimed by remember { mutableStateOf<Int?>(null) }
    Dialog({ act.checkin = false }) {
        Column(Modifier.width(300.dp).clip(RoundedCornerShape(22.dp)).background(Color.White)) {
            Box(Modifier.fillMaxWidth().background(BrandGradient).padding(16.dp)) {
                Column {
                    Text(if (claimed != null) "Tomorrow: ${rewards[(today + 1) % 7]} 💎" else "Daily Check-in", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Check in 7 days in a row for bigger rewards", color = Color.White.copy(alpha = .85f), fontSize = 11.sp)
                }
                Text("✕", color = Color.White, modifier = Modifier.align(Alignment.TopEnd).clickable { act.checkin = false })
            }
            Column(Modifier.padding(12.dp)) {
                (0 until 7).chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { i ->
                            val done = i < today || (i == today && (claimed != null || !Store.canCheckin))
                            val isToday = i == today && Store.canCheckin
                            Column(
                                Modifier.weight(if (i == 6) 2f else 1f).clip(RoundedCornerShape(10.dp))
                                    .background(when { done -> Color(0xFFE6F9EF); i == 6 -> Color(0xFFFFF3D6); else -> Lilac })
                                    .border(1.5.dp, if (isToday) Purple else Color.Transparent, RoundedCornerShape(10.dp)).padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Day${i + 1}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (done) Color(0xFF0A7A5C) else LilacText)
                                Text(if (done) "✓" else if (i == 6) "🎁" else "💎", fontSize = 18.sp)
                                Text("+${rewards[i]}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (done) Color(0xFF0A7A5C) else LilacText)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (claimed != null) Text("🎉 Gems x $claimed added!", fontWeight = FontWeight.Bold, color = Green, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp))
                else if (Store.canCheckin) GradientButton("Claim") { claimed = Store.claimCheckin() }
                else Text("Already claimed today. Come back tomorrow!", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp))
            }
        }
    }
}
