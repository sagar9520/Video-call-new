package com.myfuturecode.chatsetu

import android.app.DatePickerDialog
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.Period
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

// ---------------- Splash ----------------

@Composable
fun SplashScreen(act: MainActivity) {
    val anim = rememberInfiniteTransition(label = "s")
    val ring by anim.animateFloat(1f, 1.5f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "r")
    val bar by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "b")
    LaunchedEffect(Unit) {
        withTimeoutOrNull(6000) { RemoteConfig.refresh(act) }
        Ads.preload(act)
        seedWelcomeMessages()
        Ads.loadAppOpen()
        withTimeoutOrNull(4000) { while (!Ads.appOpenReady) delay(200) }
        Ads.showAppOpen(act) {
            (act.application as App).splashDone = true
            act.reset(if (Store.onboarded) Route.Main() else Route.Onboarding)
        }
    }
    DarkStatusBar()
    Box(Modifier.fillMaxSize().background(BrandGradient), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(112.dp).scale(ring).clip(CircleShape).background(Color.White.copy(alpha = (1.5f - ring) * .5f)))
                Box(Modifier.size(112.dp).shadow(16.dp, CircleShape).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Videocam, null, tint = Purple, modifier = Modifier.size(56.dp))
                }
            }
            Text("ChatSetu Live", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 26.dp))
            Text("Video chat · Make new friends", color = Color.White.copy(alpha = .8f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Box(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp).width(150.dp).height(4.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = .2f)),
        ) { Box(Modifier.fillMaxWidth(bar).fillMaxHeight().clip(CircleShape).background(Color.White)) }
    }
}

/** Pehli baar: har online host ka pehla script message inbox me (unread). */
private fun seedWelcomeMessages() {
    val cfg = RemoteConfig.current
    cfg.hosts.filter { it.online && it.id !in Store.blocked && Store.chats[it.id].isNullOrEmpty() }.take(3).forEach { h ->
        val first = (h.script ?: cfg.chatScript).firstOrNull() ?: return@forEach
        Store.addMsg(h.id, ChatMsg(false, first.type, first.text, first.url, first.duration, System.currentTimeMillis()), countUnread = true)
        Store.advanceScript(h.id)
    }
}

// ---------------- Onboarding (18+ gate) ----------------

val allInterests = listOf("🎵 Music", "🏏 Cricket", "🎬 Movies", "✈ Travel", "🎮 Gaming", "📚 Study", "🗣 English practice", "🍳 Cooking", "💪 Fitness")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(act: MainActivity) {
    var name by remember { mutableStateOf(Store.name.value) }
    var dob by remember { mutableStateOf<LocalDate?>(null) }
    var gender by remember { mutableStateOf("Male") }
    val tags = remember { mutableStateListOf<String>() }
    var agree by remember { mutableStateOf(false) }
    val age = dob?.let { Period.between(it, LocalDate.now()).years }
    DarkStatusBar()
    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).background(BrandGradient)
                .statusBarsPadding().padding(top = 24.dp, bottom = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(76.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Videocam, null, tint = Purple, modifier = Modifier.size(38.dp))
                }
                Text("Welcome to ChatSetu Live", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
                Text("Create your profile to start", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
            }
        }
        Column(Modifier.padding(20.dp)) {
            Label("Your name")
            Field(name, "Enter your name") { name = it.take(20) }
            Label("Date of birth")
            Row(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF5F1FA)).clickable {
                    val c = Calendar.getInstance().apply { add(Calendar.YEAR, -20) }
                    DatePickerDialog(act, { _, y, m, d -> dob = LocalDate.of(y, m + 1, d) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
                        .apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Cake, null, tint = Purple, modifier = Modifier.size(20.dp))
                Text(dob?.let { "${it.dayOfMonth}/${it.monthValue}/${it.year}" } ?: "Select date", color = if (dob == null) Color.Gray else Ink,
                    modifier = Modifier.padding(start = 10.dp).weight(1f))
                if (age != null) Text(if (age >= 18) "18+ ✓" else "Under 18", color = if (age >= 18) Green else Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Label("Gender")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Male", "Female", "Other").forEach { g -> Pill(g, gender == g, Modifier.weight(1f)) { gender = g } }
            }
            Label("Interests")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allInterests.forEach { t -> Pill(t, t in tags) { if (t in tags) tags.remove(t) else tags.add(t) } }
            }
        }
    }
        // Neeche hamesha dikhne wala hissa: 18+ checkbox + Continue
        Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth().clickable { agree = !agree }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(agree, { agree = it }, colors = CheckboxDefaults.colors(checkedColor = Purple))
                Text("I am 18+ and agree to the Terms, Privacy Policy and Community Guidelines", color = Color.Gray, fontSize = 12.sp)
            }
            GradientButton("Continue") {
                when {
                    name.isBlank() -> act.toast("Please enter your name")
                    age == null -> act.toast("Please select your date of birth")
                    age < 18 -> act.toast("You must be 18 or older to use ChatSetu Live")
                    !agree -> act.toast("Please accept the terms")
                    else -> {
                        Store.saveProfile(name.trim(), dob.toString(), gender, tags.toList())
                        act.reset(Route.Main())
                    }
                }
            }
        }
    }
}

@Composable
fun Label(t: String) = Text(t, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

@Composable
fun Field(value: String, hint: String, lines: Int = 1, onChange: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF5F1FA)).padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Text(hint, color = Color.Gray, fontSize = 15.sp)
        BasicTextField(value, onChange, Modifier.fillMaxWidth(), singleLine = lines == 1, maxLines = lines,
            textStyle = TextStyle(fontSize = 15.sp, color = Ink), cursorBrush = SolidColor(Purple))
    }
}

@Composable
fun Pill(text: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text, color = if (on) Color.White else Ink, fontSize = 13.sp, textAlign = TextAlign.Center, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(if (on) ButtonGradient else SolidColor(Color(0xFFF5F1FA)))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

// ---------------- Main (bottom tabs) ----------------

@Composable
fun MainScreen(act: MainActivity) {
    val unreadTotal = Store.unread.values.sum()
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.weight(1f)) {
            when (act.mainTab) {
                0 -> DiscoverTab(act)
                1 -> MatchTab(act)
                2 -> MessagesTab(act)
                else -> MeTab(act)
            }
        }
        Column(Modifier.background(Color.White)) {
            HorizontalDivider(color = Color(0xFFF0ECF5))
            Row(Modifier.fillMaxWidth().navigationBarsPadding().height(60.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                NavItem(Icons.Filled.Explore, Icons.Outlined.Explore, "Discover", act.mainTab == 0) { act.mainTab = 0 }
                NavItem(Icons.Filled.Shuffle, Icons.Outlined.Shuffle, "Match", act.mainTab == 1) { act.mainTab = 1 }
                NavItem(Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, "Messages", act.mainTab == 2, unreadTotal) { act.mainTab = 2 }
                NavItem(Icons.Filled.Person, Icons.Outlined.Person, "Me", act.mainTab == 3) { act.mainTab = 3 }
            }
        }
    }
}

@Composable
private fun NavItem(on: ImageVector, off: ImageVector, label: String, selected: Boolean, badge: Int = 0, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(if (selected) on else off, label, tint = if (selected) Purple else Color(0xFFA9A4B3), modifier = Modifier.size(25.dp))
            if (badge > 0) Text(if (badge > 99) "99+" else "$badge", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 9.dp, y = (-4).dp).clip(CircleShape).background(Red).padding(horizontal = 4.dp, vertical = 1.dp))
        }
        Text(label, fontSize = 11.sp, color = if (selected) Purple else Color(0xFFA9A4B3), fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun Header(act: MainActivity, title: String, extra: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 14.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.weight(1f))
        extra()
        GemChip(act)
    }
}

// ----- Discover -----

@Composable
private fun DiscoverTab(act: MainActivity) {
    val cfg = RemoteConfig.config.collectAsStateValue() ?: return
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var searching by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    val tabs = listOf("Popular", "New", "Following", "Nearby")
    val hosts = cfg.hosts.filter { it.id !in Store.blocked }.let { all ->
        val base = when (tab) {
            1 -> all.reversed()
            2 -> Store.following.mapNotNull { id -> all.firstOrNull { it.id == id } }
            3 -> all.sortedBy { it.city }
            else -> all.sortedWith(compareByDescending<Host> { it.online }.thenByDescending { it.followers })
        }
        if (q.isBlank()) base else base.filter {
            it.name.contains(q, true) || it.city.contains(q, true) || it.lang.contains(q, true) || it.id.contains(q, true) || it.tags.any { t -> t.contains(q, true) }
        }
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Header(act, "Discover") {
            IconButton({ searching = !searching; if (!searching) q = "" }) {
                Icon(if (searching) Icons.Default.Close else Icons.Default.Search, "Search", tint = Ink)
            }
        }
        if (searching) Row(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFF5F1FA)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
            Box(Modifier.weight(1f).padding(start = 8.dp)) {
                if (q.isEmpty()) Text("Search name, city, language or ID", color = Color.Gray, fontSize = 14.sp)
                BasicTextField(q, { q = it }, singleLine = true, textStyle = TextStyle(fontSize = 14.sp, color = Ink), cursorBrush = SolidColor(Purple),
                    modifier = Modifier.fillMaxWidth())
            }
        }
        Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            tabs.forEachIndexed { i, t ->
                Column(Modifier.clickable { tab = i }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t, fontWeight = if (tab == i) FontWeight.ExtraBold else FontWeight.Medium, fontSize = if (tab == i) 16.sp else 15.sp, color = if (tab == i) Ink else Color(0xFF9A95A3))
                    Box(Modifier.padding(top = 4.dp).size(width = 18.dp, height = 3.dp).clip(CircleShape).background(if (tab == i) ButtonGradient else SolidColor(Color.Transparent)))
                }
            }
        }
        LazyVerticalGrid(
            GridCells.Fixed(2), Modifier.weight(1f), contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (tab == 0 && q.isBlank() && Store.canCheckin) item(span = { GridItemSpan(2) }) { CheckinBanner(act) }
            items(hosts, key = { it.id }) { h -> HostCard(act, h) }
            if (hosts.isEmpty()) item(span = { GridItemSpan(2) }) {
                Text(
                    if (tab == 2 && q.isBlank()) "You are not following anyone yet.\nOpen a profile and tap Follow." else "No one found",
                    color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                )
            }
        }
        Ads.Banner()
    }
}

@Composable
private fun CheckinBanner(act: MainActivity) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFFB52E), Color(0xFFFF6A3D))))
            .clickable { act.checkin = true }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🎁", fontSize = 26.sp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("Daily check-in", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text("Claim free gems today", color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
        }
        Text("Claim", color = Color(0xFFFF6A3D), fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 6.dp))
    }
}

@Composable
private fun HostCard(act: MainActivity, h: Host) {
    val cfg = RemoteConfig.current
    Box(Modifier.fillMaxWidth().aspectRatio(.72f).shadow(4.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).clickable { act.push(Route.Profile(h.id)) }) {
        HostImage(h, Modifier.fillMaxSize(), letterSize = 64.sp)
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) { VirtualTag(h); OnlineDot(h.online) }
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .72f)))).padding(start = 10.dp, end = 8.dp, top = 26.dp, bottom = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("${h.name}, ${h.age}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
                    Text("🇮🇳 ${h.city}", color = Color.White.copy(alpha = .85f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    Modifier.size(38.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(CallGradient).clickable { act.startCall(h.id) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Videocam, "Video call", tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "👋 Say Hi", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .22f)).clickable {
                        if (!Store.spend(cfg.msgCost)) { act.needGems(); return@clickable }
                        Store.addMsg(h.id, ChatMsg(true, "text", "Hi 👋", "", 0, System.currentTimeMillis()), countUnread = false)
                        act.toast("Hello sent to ${h.name}")
                    }.padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                Text("${h.rate}💎/min", color = Color.White.copy(alpha = .85f), fontSize = 10.sp)
            }
        }
    }
}

// ----- Match -----

@Composable
private fun MatchTab(act: MainActivity) {
    val cfg = RemoteConfig.current
    val scope = rememberCoroutineScope()
    var searching by remember { mutableStateOf(false) }
    var genderFilter by remember { mutableStateOf("Any") }
    val anim = rememberInfiniteTransition(label = "w")
    val wave by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "s")
    val spin by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")
    val pool = cfg.hosts.filter { it.online && it.id !in Store.blocked }
    DarkStatusBar()
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A1260), Color(0xFF130A33)))), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 14.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Match", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.weight(1f))
            GemChip(act, dark = true)
        }
        Text("${pool.size * 37 + 120} people online now", color = Color(0xFFB9B1E0), fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            repeat(3) { i ->
                val f = (wave + i / 3f) % 1f
                Box(Modifier.size(150.dp).scale(1f + f).clip(CircleShape).border(1.5.dp, Color.White.copy(alpha = (1f - f) * .5f), CircleShape))
            }
            // Online hosts ke avatar ghoomte hue
            Box(Modifier.fillMaxSize().rotate(spin)) {
                pool.take(6).forEachIndexed { i, h ->
                    val a = Math.toRadians(i * 60.0)
                    HostImage(
                        h, Modifier.align(Alignment.Center).offset(x = (120 * cos(a)).dp, y = (120 * sin(a)).dp).size(44.dp).rotate(-spin)
                            .clip(CircleShape).border(2.dp, Color.White, CircleShape),
                        letterSize = 18.sp,
                    )
                }
            }
            Box(
                Modifier.size(150.dp).shadow(20.dp, CircleShape).clip(CircleShape).background(CallGradient).clickable(enabled = !searching) {
                    if (pool.isEmpty()) { act.toast("No one is online right now"); return@clickable }
                    if (genderFilter != "Any" && !Store.spend(cfg.filterCost)) { act.needGems(); return@clickable }
                    searching = true
                    scope.launch {
                        delay(2200L + (0..1500).random())
                        searching = false
                        Store.addMatch()
                        Ads.maybeInterstitial(act) { act.startCall(pool.random().id, fromMatch = true) }
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (searching) CircularProgressIndicator(Modifier.size(40.dp), color = Color.White, strokeWidth = 3.dp)
                    else Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(46.dp))
                    Text(if (searching) "Matching…" else "Start Match", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Gender filter", color = Color(0xFFB9B1E0), fontSize = 12.sp)
        Row(Modifier.padding(top = 8.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Any", "Female", "Male").forEach { g ->
                Text(
                    if (g == "Any") g else "$g · ${cfg.filterCost}💎", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(if (genderFilter == g) ButtonGradient else SolidColor(Color.White.copy(alpha = .1f)))
                        .clickable { genderFilter = g }.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        Ads.Banner()
    }
}

// ----- Messages -----

@Composable
private fun MessagesTab(act: MainActivity) {
    val cfg = RemoteConfig.current
    var seg by rememberSaveable { mutableIntStateOf(0) }
    val rows = cfg.hosts.filter { !Store.chats[it.id].isNullOrEmpty() && it.id !in Store.blocked }
        .sortedByDescending { Store.chats[it.id]?.lastOrNull()?.time ?: 0 }
    val follows = Store.following.mapNotNull { id -> cfg.hosts.firstOrNull { it.id == id && it.id !in Store.blocked } }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Header(act, "Messages")
        // Online hosts ki story-jaisi line
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(cfg.hosts.filter { it.online && it.id !in Store.blocked }) { h ->
                Column(Modifier.clickable { act.push(Route.Profile(h.id)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        HostImage(h, Modifier.size(58.dp).border(2.dp, CallGradient, CircleShape).padding(3.dp).clip(CircleShape), letterSize = 22.sp)
                        Box(Modifier.align(Alignment.BottomEnd).size(14.dp).clip(CircleShape).background(Color.White).padding(2.dp).clip(CircleShape).background(Green))
                    }
                    Text(h.name, fontSize = 11.sp, color = Ink, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFF5F1FA)).padding(3.dp)) {
            listOf("Chats", "Following (${follows.size})").forEachIndexed { i, t ->
                Text(t, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (seg == i) Color.White else Color.Gray, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (seg == i) ButtonGradient else SolidColor(Color.Transparent))
                        .clickable { seg = i }.padding(vertical = 8.dp))
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            if (seg == 0) {
                item {
                    Row(Modifier.fillMaxWidth().clickable { act.checkin = true }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clip(CircleShape).background(BrandGradient), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.NotificationsActive, null, tint = Color.White)
                        }
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text("Team ChatSetu", fontWeight = FontWeight.Bold, color = Ink)
                            Text(if (Store.canCheckin) "Your daily check-in reward is ready 🎁" else "Welcome! Be kind and stay safe.", fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                        }
                        if (Store.canCheckin) Box(Modifier.size(10.dp).clip(CircleShape).background(Red))
                    }
                }
                if (rows.isEmpty()) item {
                    Text("No messages yet. Open a profile and say hi!", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp))
                }
                items(rows, key = { it.id }) { h ->
                    val last = Store.chats[h.id]!!.last()
                    val unread = Store.unread[h.id] ?: 0
                    Row(Modifier.fillMaxWidth().clickable { act.push(Route.Chat(h.id)) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            HostImage(h, Modifier.size(52.dp).clip(CircleShape), letterSize = 20.sp)
                            if (h.online) Box(Modifier.align(Alignment.BottomEnd).size(13.dp).clip(CircleShape).background(Color.White).padding(2.dp).clip(CircleShape).background(Green))
                        }
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(h.name, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.size(6.dp)); VirtualTag(h, dark = false)
                            }
                            Text(preview(last), fontSize = 13.sp, color = if (unread > 0) Ink else Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(timeAgo(last.time), fontSize = 11.sp, color = Color.Gray)
                            if (unread > 0) Text("$unread", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp).clip(CircleShape).background(Red).padding(horizontal = 7.dp, vertical = 2.dp))
                        }
                    }
                }
            } else {
                if (follows.isEmpty()) item {
                    Text("Tap Follow on a profile to see them here", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp))
                }
                items(follows, key = { it.id }) { h ->
                    Row(Modifier.fillMaxWidth().clickable { act.push(Route.Profile(h.id)) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        HostImage(h, Modifier.size(52.dp).clip(CircleShape), letterSize = 20.sp)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(h.name, fontWeight = FontWeight.Bold, color = Ink)
                            Text(if (h.online) "● Online" else "Offline", fontSize = 12.sp, color = if (h.online) Green else Color.Gray)
                        }
                        RoundIcon(Icons.AutoMirrored.Filled.Chat, Lilac, Purple) { act.push(Route.Chat(h.id)) }
                        Spacer(Modifier.width(8.dp))
                        RoundIcon(Icons.Default.Videocam, Pink.copy(alpha = .12f), Pink) { act.startCall(h.id) }
                    }
                }
            }
        }
    }
}

@Composable
fun RoundIcon(icon: ImageVector, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

fun timeAgo(t: Long): String {
    val m = (System.currentTimeMillis() - t) / 60000
    return when { m < 1 -> "now"; m < 60 -> "${m}m"; m < 1440 -> "${m / 60}h"; else -> "${m / 1440}d" }
}

fun preview(m: ChatMsg): String = (if (m.mine) "You: " else "") + when (m.type) {
    "image" -> "📷 Photo"; "voice" -> "🎤 Voice message"; "video" -> "🎬 Video"; "gift" -> "🎁 ${m.text}"; else -> m.text
}

// ----- Me -----

@Composable
fun UserAvatar(size: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(size.dp).clip(CircleShape).background(Color.White.copy(alpha = .25f)), contentAlignment = Alignment.Center) {
        if (Store.avatar.value.isNotBlank()) AsyncImage(Store.avatar.value, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(Store.name.value.take(1).uppercase(), color = Color.White, fontSize = (size * .42f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MeTab(act: MainActivity) {
    val cfg = RemoteConfig.current
    var safety by remember { mutableStateOf(false) }
    var deleteAsk by remember { mutableStateOf(false) }
    var promo by remember { mutableStateOf(false) }
    DarkStatusBar()
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F5FA)).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).background(BrandGradient).statusBarsPadding().padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 50.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(66, Modifier.border(2.dp, Color.White, CircleShape).clickable { act.push(Route.EditProfile) })
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(Store.name.value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                    Text("ID ${Store.userId.value}", color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
                    if (Store.bio.value.isNotBlank()) Text(Store.bio.value, color = Color.White.copy(alpha = .9f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton({ act.push(Route.EditProfile) }) { Icon(Icons.Outlined.Edit, "Edit profile", tint = Color.White) }
            }
        }
        Column(Modifier.offset(y = (-34).dp).padding(horizontal = 14.dp)) {
            Row(
                Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Color.White).padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat("${Store.following.size}", "Following") { act.mainTab = 2 }
                Stat("${Store.matches.intValue}", "Matches") { act.mainTab = 1 }
                Stat("${Store.gems.intValue}", "Gems 💎") { act.recharge = "" }
            }
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFFF3D6), Color(0xFFFFE3EC))))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("💎", fontSize = 26.sp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("${Store.gems.intValue} gems", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Ink)
                    Text("Use gems for calls, messages and gifts", fontSize = 11.sp, color = Color.Gray)
                }
                Box(Modifier.clip(RoundedCornerShape(18.dp)).background(ButtonGradient).clickable { act.recharge = "" }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                    Text("Recharge", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            MeCard {
                MeRow(Icons.Outlined.CardGiftcard, Color(0xFFFF8A3D), "Daily check-in", if (Store.canCheckin) "Ready" else null) { act.checkin = true }
                MeRow(Icons.Outlined.PlayCircle, Green, "Free gems", "Watch a video") {
                    Ads.showRewarded(act, onReward = { Store.addGems(cfg.rewardedGems); act.toast("+${cfg.rewardedGems} gems") }, onFail = { act.toast("Ad not ready, try again") })
                }
                MeRow(Icons.AutoMirrored.Outlined.ReceiptLong, Color(0xFF4A7BF7), "Purchase history") { act.push(Route.Purchases) }
                MeRow(Icons.Outlined.Edit, Purple, "Edit profile") { act.push(Route.EditProfile) }
            }
            MeCard {
                MeRow(Icons.Outlined.Block, Color(0xFF8A8A93), "Blocked", "${Store.blocked.size}") { act.push(Route.Blocked) }
                MeRow(Icons.Outlined.Shield, Green, "Safety centre") { safety = true }
                if (cfg.privacyUrl.isNotBlank()) MeRow(Icons.Outlined.Lock, Color(0xFF4A7BF7), "Privacy policy") { openUrl(act, cfg.privacyUrl) }
                if (cfg.crossPromo.isNotEmpty()) MeRow(Icons.Outlined.Apps, Color(0xFFFF8A3D), "More apps") { promo = true }
                MeRow(Icons.Outlined.StarRate, Color(0xFFFFB52E), "Rate us") { openStore(act, act.packageName) }
                MeRow(Icons.Outlined.DeleteOutline, Red, "Delete account", danger = true) { deleteAsk = true }
            }
            Text("Version ${BuildConfig.VERSION_NAME}", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), textAlign = TextAlign.Center)
        }
    }
    if (safety) AlertDialog(
        onDismissRequest = { safety = false },
        containerColor = Color.White,
        title = { Text("Safety centre") },
        text = {
            Text(
                "• ChatSetu Live is for adults (18+) only.\n• Hosts marked \"${cfg.virtualLabel}\" are demo/virtual profiles, not real people.\n" +
                    "• Never share passwords, OTPs or bank details.\n• Nudity, harassment and hate are not allowed.\n• Use Report / Block on any profile or call.",
                fontSize = 14.sp,
            )
        },
        confirmButton = { TextButton({ safety = false }) { Text("OK") } },
    )
    if (deleteAsk) AlertDialog(
        onDismissRequest = { deleteAsk = false },
        containerColor = Color.White,
        title = { Text("Delete account?") },
        text = { Text("Your profile, chats, gems and purchase history on this device will be deleted. This can't be undone.") },
        confirmButton = { TextButton({ deleteAsk = false; Store.deleteAll(act); RemoteConfig.loadLocal(act); act.mainTab = 0; act.reset(Route.Onboarding) }) { Text("Delete", color = Red) } },
        dismissButton = { TextButton({ deleteAsk = false }) { Text("Cancel") } },
    )
    if (promo) AlertDialog(
        onDismissRequest = { promo = false },
        containerColor = Color.White,
        title = { Text("More apps") },
        text = {
            LazyColumn {
                items(cfg.crossPromo) { a ->
                    Row(Modifier.fillMaxWidth().clickable { openStore(act, a.pkg) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(a.icon, a.name, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(a.name, fontWeight = FontWeight.SemiBold)
                            if (a.desc.isNotBlank()) Text(a.desc, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ promo = false }) { Text("Close") } },
    )
}

@Composable
private fun MeCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(vertical = 4.dp), content = content)
}

@Composable
private fun Stat(value: String, label: String, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Ink)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun MeRow(icon: ImageVector, tint: Color, title: String, value: String? = null, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Text(title, modifier = Modifier.padding(start = 12.dp).weight(1f), color = if (danger) Red else Ink, fontSize = 15.sp)
        if (value != null) Text(value, color = Color.Gray, fontSize = 13.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
    }
}

// ----- Edit profile -----

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(act: MainActivity) {
    var name by remember { mutableStateOf(Store.name.value) }
    var bio by remember { mutableStateOf(Store.bio.value) }
    var gender by remember { mutableStateOf(Store.gender.value.ifBlank { "Male" }) }
    var photo by remember { mutableStateOf(Store.avatar.value) }
    val tags = remember { Store.interests.toMutableStateList() }
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { act.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            photo = uri.toString()
        }
    }
    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        TopBar("Edit profile", onBack = { act.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp).clickable {
                pick.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Box(Modifier.size(96.dp).clip(CircleShape).background(BrandGradient), contentAlignment = Alignment.Center) {
                    if (photo.isNotBlank()) AsyncImage(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Text(name.take(1).uppercase(), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.align(Alignment.BottomEnd).size(30.dp).clip(CircleShape).background(Color.White).padding(3.dp).clip(CircleShape).background(Purple), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PhotoCamera, "Change photo", tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
            Label("Name")
            Field(name, "Your name") { name = it.take(20) }
            Label("About me")
            Field(bio, "Write something about you", lines = 3) { bio = it.take(120) }
            Label("Gender")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Male", "Female", "Other").forEach { g -> Pill(g, gender == g, Modifier.weight(1f)) { gender = g } }
            }
            Label("Interests")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allInterests.forEach { t -> Pill(t, t in tags) { if (t in tags) tags.remove(t) else tags.add(t) } }
            }
            Spacer(Modifier.height(20.dp))
        }
        Box(Modifier.navigationBarsPadding().padding(16.dp)) {
            GradientButton("Save") {
                if (name.isBlank()) { act.toast("Please enter your name"); return@GradientButton }
                Store.editProfile(name.trim(), bio.trim(), gender, tags.toList(), photo)
                act.toast("Profile saved"); act.back()
            }
        }
    }
}

@Composable
fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateValue(): T = collectAsState().value
