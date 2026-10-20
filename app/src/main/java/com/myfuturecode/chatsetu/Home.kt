package com.myfuturecode.chatsetu

import android.app.DatePickerDialog
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.Period
import java.util.Calendar

// ---------------- Splash ----------------

@Composable
fun SplashScreen(act: MainActivity) {
    val pulse = rememberInfiniteTransition(label = "s").animateFloat(.92f, 1.06f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p")
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
    Box(Modifier.fillMaxSize().background(BrandGradient), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(110.dp).scale(pulse.value).clip(RoundedCornerShape(30.dp)).background(Color.White.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                Text("💬", fontSize = 54.sp)
            }
            Text("ChatSetu Live", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 22.dp))
            Text("Talk live with new people", color = Color.White.copy(alpha = .8f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Text("by MyFutureCode", color = Color.White.copy(alpha = .6f), fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 20.dp))
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

private val allInterests = listOf("🎵 Music", "🏏 Cricket", "🎬 Movies", "✈ Travel", "🎮 Gaming", "📚 Study", "🗣 English practice", "🍳 Cooking", "💪 Fitness")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(act: MainActivity) {
    var name by remember { mutableStateOf(Store.name.value) }
    var dob by remember { mutableStateOf<LocalDate?>(null) }
    var gender by remember { mutableStateOf("Male") }
    val tags = remember { mutableStateListOf<String>() }
    var agree by remember { mutableStateOf(false) }
    val age = dob?.let { Period.between(it, LocalDate.now()).years }
    val field = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White.copy(alpha = .6f),
        unfocusedBorderColor = Color.White.copy(alpha = .25f), focusedLabelColor = Color.White, unfocusedLabelColor = Color.White.copy(alpha = .7f), cursorColor = Color.White,
    )
    Column(
        Modifier.fillMaxSize().background(Night).statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.padding(top = 26.dp).size(84.dp).clip(RoundedCornerShape(24.dp)).background(BrandGradient), contentAlignment = Alignment.Center) { Text("💬", fontSize = 38.sp) }
        Text("ChatSetu Live", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
        Text("Create your profile", color = Color(0xFFB9B1E0), fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))
        OutlinedTextField(name, { name = it.take(20) }, label = { Text("Your name") }, singleLine = true, colors = field, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(6.dp))
                .clickable {
                    val c = Calendar.getInstance().apply { add(Calendar.YEAR, -20) }
                    DatePickerDialog(act, { _, y, m, d -> dob = LocalDate.of(y, m + 1, d) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
                        .apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                }.padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dob?.let { "🎂 ${it.dayOfMonth}/${it.monthValue}/${it.year}" } ?: "🎂 Date of birth", color = Color.White.copy(alpha = if (dob == null) .7f else 1f), modifier = Modifier.weight(1f))
                if (age != null) Text(if (age >= 18) "18+ ✓" else "Under 18", color = if (age >= 18) Color(0xFF4CF0D0) else Red, fontWeight = FontWeight.Bold)
            }
        }
        Text("Gender", color = Color(0xFFB9B1E0), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Male", "Female", "Other").forEach { g -> Chip(g, gender == g, Modifier.weight(1f)) { gender = g } }
        }
        Text("Interests (for better matches)", color = Color(0xFFB9B1E0), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            allInterests.forEach { t -> Chip(t, t in tags) { if (t in tags) tags.remove(t) else tags.add(t) } }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(agree, { agree = it })
            Text("I am 18+ and agree to the Terms, Privacy Policy and Community Guidelines", color = Color(0xFFB9B1E0), fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
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
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(if (on) Purple else Color.White.copy(alpha = .08f))
            .border(1.dp, if (on) Purple else Color.White.copy(alpha = .2f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

// ---------------- Main (bottom tabs) ----------------

@Composable
fun MainScreen(act: MainActivity) {
    val unreadTotal = Store.unread.values.sum()
    Column(Modifier.fillMaxSize().background(PageGradient)) {
        Box(Modifier.weight(1f)) {
            when (act.mainTab) {
                0 -> DiscoverTab(act)
                1 -> MatchTab(act)
                2 -> MessagesTab(act)
                else -> MeTab(act)
            }
        }
        Row(
            Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().height(58.dp),
            horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Default.Home, "Discover", act.mainTab == 0) { act.mainTab = 0 }
            NavItem(Icons.Default.Casino, "Match", act.mainTab == 1) { act.mainTab = 1 }
            NavItem(Icons.AutoMirrored.Filled.Chat, "Messages", act.mainTab == 2, unreadTotal) { act.mainTab = 2 }
            NavItem(Icons.Default.Person, "Me", act.mainTab == 3) { act.mainTab = 3 }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, on: Boolean, badge: Int = 0, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Icon(icon, label, tint = if (on) Purple else Color(0xFFAAAAAA))
            if (badge > 0) Text("$badge", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp).clip(CircleShape).background(Red).padding(horizontal = 4.dp))
        }
        Text(label, fontSize = 10.sp, color = if (on) Purple else Color(0xFFAAAAAA), fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun Header(act: MainActivity, title: String) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        GemChip(act)
    }
}

// ----- Discover -----

@Composable
private fun DiscoverTab(act: MainActivity) {
    val cfg = RemoteConfig.config.collectAsStateValue() ?: return
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Popular", "New", "Online", "Hindi")
    val hosts = cfg.hosts.filter { it.id !in Store.blocked }.let { all ->
        when (tab) {
            1 -> all.reversed()
            2 -> all.filter { it.online }
            3 -> all.filter { it.lang.contains("Hindi", true) }
            else -> all.sortedByDescending { it.online }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Header(act, "Discover")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            tabs.forEachIndexed { i, t ->
                Column(Modifier.clickable { tab = i }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (tab == i) Color(0xFF222222) else Color(0xFF999999))
                    Box(Modifier.padding(top = 3.dp).size(width = 22.dp, height = 3.dp).clip(CircleShape).background(if (tab == i) Purple else Color.Transparent))
                }
            }
        }
        LazyVerticalGrid(
            GridCells.Fixed(2), Modifier.weight(1f), contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(hosts, key = { it.id }) { h -> HostCard(act, h) }
        }
        Ads.Banner()
    }
}

@Composable
private fun HostCard(act: MainActivity, h: Host) {
    Box(Modifier.fillMaxWidth().aspectRatio(.78f).clip(RoundedCornerShape(16.dp)).clickable { act.push(Route.Profile(h.id)) }) {
        HostImage(h, Modifier.fillMaxSize(), letterSize = 64.sp)
        Row(Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.SpaceBetween) { VirtualTag(h); OnlineDot(h.online) }
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .6f))))
                .padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("${h.name}, ${h.age}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("🇮🇳 ${h.lang}", color = Color.White.copy(alpha = .85f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(34.dp).clip(CircleShape).background(CallGradient).clickable { act.push(Route.Call(h.id)) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Videocam, "Video call", tint = Color.White, modifier = Modifier.size(18.dp))
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
    val wave = rememberInfiniteTransition(label = "w").animateFloat(1f, 1.25f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Header(act, "Match")
        if (Store.canCheckin) Row(
            Modifier.padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GoldGradient).clickable { act.checkin = true }.padding(12.dp),
        ) {
            Text("🎁 Daily check-in reward is ready", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("Claim ›", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(200.dp).scale(if (searching) wave.value else 1f).clip(CircleShape).background(Purple.copy(alpha = .12f)))
            Box(
                Modifier.size(170.dp).clip(CircleShape).background(BrandGradient).clickable(enabled = !searching) {
                    val pool = cfg.hosts.filter { it.online && it.id !in Store.blocked }
                    if (pool.isEmpty()) { act.toast("No one is online right now"); return@clickable }
                    if (genderFilter != "Any" && !Store.spend(cfg.filterCost)) { act.needGems(); return@clickable }
                    searching = true
                    scope.launch {
                        delay(2200L + (0..1500).random())
                        searching = false
                        Store.addMatch()
                        Ads.maybeInterstitial(act) { act.push(Route.Call(pool.random().id, fromMatch = true)) }
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(46.dp))
                    Text(if (searching) "Searching…" else "Start Match", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                }
            }
        }
        Text("Random video match with hosts online now", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 26.dp))
        Spacer(Modifier.weight(1f))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Gender:", fontSize = 13.sp, color = Color.Gray)
            listOf("Any", "Female", "Male").forEach { g ->
                Text(
                    if (g == "Any") g else "$g 💎${cfg.filterCost}", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (genderFilter == g) Color.White else LilacText,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (genderFilter == g) Purple else Lilac)
                        .clickable { genderFilter = g }.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Ads.Banner()
    }
}

// ----- Messages -----

@Composable
private fun MessagesTab(act: MainActivity) {
    val cfg = RemoteConfig.current
    val rows = cfg.hosts.filter { !Store.chats[it.id].isNullOrEmpty() && it.id !in Store.blocked }
        .sortedByDescending { Store.chats[it.id]?.lastOrNull()?.time ?: 0 }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Header(act, "Messages")
        LazyColumn(Modifier.weight(1f)) {
            if (Store.canCheckin) item {
                Row(Modifier.fillMaxWidth().clickable { act.checkin = true }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(BrandGradient), contentAlignment = Alignment.Center) { Text("🔔", fontSize = 20.sp) }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Team ChatSetu", fontWeight = FontWeight.Bold)
                        Text("Your daily check-in reward is ready", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                HorizontalDivider(color = Color(0xFFF2F2F2))
            }
            if (rows.isEmpty()) item {
                Text("No messages yet. Open a profile and say hi!", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp))
            }
            items(rows, key = { it.id }) { h ->
                val last = Store.chats[h.id]!!.last()
                val unread = Store.unread[h.id] ?: 0
                Row(Modifier.fillMaxWidth().clickable { act.push(Route.Chat(h.id)) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    HostImage(h, Modifier.size(48.dp).clip(CircleShape), letterSize = 20.sp)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(h.name, fontWeight = FontWeight.Bold); Spacer(Modifier.size(6.dp)); VirtualTag(h, dark = false)
                        }
                        Text(preview(last), fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (unread > 0) Text("$unread", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(CircleShape).background(Red).padding(horizontal = 7.dp, vertical = 2.dp))
                }
                HorizontalDivider(color = Color(0xFFF2F2F2))
            }
        }
    }
}

fun preview(m: ChatMsg): String = (if (m.mine) "You: " else "") + when (m.type) {
    "image" -> "📷 Photo"; "voice" -> "🎤 Voice message"; "video" -> "🎬 Video"; "gift" -> "🎁 ${m.text}"; else -> m.text
}

// ----- Me -----

@Composable
private fun MeTab(act: MainActivity) {
    val cfg = RemoteConfig.current
    var safety by remember { mutableStateOf(false) }
    var deleteAsk by remember { mutableStateOf(false) }
    var promo by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().background(BrandGradient).statusBarsPadding().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 34.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = .2f)), contentAlignment = Alignment.Center) {
                Text(Store.name.value.take(1).uppercase(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(Store.name.value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("ID ${Store.userId.value} · ${Store.matches.intValue} matches", color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
            }
        }
        Row(
            Modifier.offset(y = (-20).dp).padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                .border(1.dp, Color(0xFFEDE7FB), RoundedCornerShape(16.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("💎 ", fontSize = 20.sp)
            Text("${Store.gems.intValue}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text("  gems", color = Color.Gray, modifier = Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(18.dp)).background(ButtonGradient).clickable { act.recharge = "" }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Recharge", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        MeRow("🎁", "Daily check-in") { act.checkin = true }
        MeRow("▶", "Free gems (watch ad)") {
            Ads.showRewarded(act, onReward = { Store.addGems(cfg.rewardedGems); act.toast("+${cfg.rewardedGems} gems") }, onFail = { act.toast("Ad not ready, try again") })
        }
        MeRow("🧾", "Purchase history") { act.push(Route.Purchases) }
        MeRow("🚫", "Blocked (${Store.blocked.size})") { act.push(Route.Blocked) }
        MeRow("🛡", "Safety centre & guidelines") { safety = true }
        if (cfg.privacyUrl.isNotBlank()) MeRow("🔒", "Privacy policy") { openUrl(act, cfg.privacyUrl) }
        if (cfg.crossPromo.isNotEmpty()) MeRow("📱", "More apps") { promo = true }
        MeRow("⭐", "Rate us") { openStore(act, act.packageName) }
        MeRow("🗑", "Delete account", danger = true) { deleteAsk = true }
        Text("Version ${BuildConfig.VERSION_NAME}", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
    }
    if (safety) AlertDialog(
        onDismissRequest = { safety = false },
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
        title = { Text("Delete account?") },
        text = { Text("Your profile, chats, gems and purchase history on this device will be deleted. This can't be undone.") },
        confirmButton = { TextButton({ deleteAsk = false; Store.deleteAll(act); RemoteConfig.loadLocal(act); act.mainTab = 0; act.reset(Route.Onboarding) }) { Text("Delete", color = Red) } },
        dismissButton = { TextButton({ deleteAsk = false }) { Text("Cancel") } },
    )
    if (promo) AlertDialog(
        onDismissRequest = { promo = false },
        title = { Text("More apps") },
        text = {
            LazyColumn {
                items(cfg.crossPromo) { a ->
                    Row(Modifier.fillMaxWidth().clickable { openStore(act, a.pkg) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        coil.compose.AsyncImage(a.icon, a.name, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
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
private fun MeRow(icon: String, title: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Text(title, modifier = Modifier.padding(start = 12.dp).weight(1f), color = if (danger) Red else Color(0xFF222222), fontSize = 15.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
    }
    HorizontalDivider(color = Color(0xFFF4F4F4))
}

@Composable
fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateValue(): T = collectAsState().value
