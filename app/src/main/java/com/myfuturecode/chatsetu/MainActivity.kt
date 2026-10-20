package com.myfuturecode.chatsetu

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

sealed class Route {
    data object Splash : Route()
    data object Onboarding : Route()
    data class Main(val tab: Int = 0) : Route()
    data class Profile(val hostId: String) : Route()
    data class Chat(val hostId: String) : Route()
    data class Call(val hostId: String, val fromMatch: Boolean = false, val audio: Boolean = false) : Route()
    data object Purchases : Route()
    data object Blocked : Route()
    data object EditProfile : Route()
}

class MainActivity : ComponentActivity() {
    val stack = mutableStateListOf<Route>(Route.Splash)
    val route get() = stack.last()

    // Poori app ke upar khulne wale popups
    var recharge by mutableStateOf<String?>(null)      // null = band; text = upar ka message
    var lowGems by mutableStateOf(false)
    var checkin by mutableStateOf(false)
    var mainTab by mutableStateOf(0)
    var connectingHost by mutableStateOf<Host?>(null)
    var onlineSuggest by mutableStateOf<Host?>(null)
    var banner by mutableStateOf<Pair<Host, String>?>(null)   // "Connecting" pill (original jaisa)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("ci_no_ads", false) == true) Ads.ciNoAds = true
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE),
        )
        Billing.onMessage = { msg -> runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() } }
        setContent {
            ChatSetuTheme {
                BackHandler(enabled = stack.size > 1 || (route is Route.Main && mainTab != 0)) { back() }
                AnimatedContent(route, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "nav") { r ->
                    when (r) {
                        Route.Splash -> SplashScreen(this@MainActivity)
                        Route.Onboarding -> OnboardingScreen(this@MainActivity)
                        is Route.Main -> MainScreen(this@MainActivity)
                        is Route.Profile -> ProfileScreen(this@MainActivity, r.hostId)
                        is Route.Chat -> ChatScreen(this@MainActivity, r.hostId)
                        is Route.Call -> CallScreen(this@MainActivity, r.hostId, r.fromMatch, r.audio)
                        Route.Purchases -> PurchasesScreen(this@MainActivity)
                        Route.Blocked -> BlockedScreen(this@MainActivity)
                        Route.EditProfile -> EditProfileScreen(this@MainActivity)
                    }
                }
                LaunchedEffect(route) {
                    if (route is Route.Main && Store.canCheckin && Store.onboarded) checkin = true
                }
                Overlays(this@MainActivity)
            }
        }
    }

    private var demoStarted = false

    override fun onResume() {
        super.onResume()
        if (!demoStarted) { demoStarted = true; lifecycleScope.launch { kotlinx.coroutines.delay(15000); demoActivityLoop() } }
        // Cashfree se wapas aaye to pending order check
        lifecycleScope.launch { Billing.verifyPending() }
    }

    fun push(r: Route) { stack.add(r) }

    fun replace(r: Route) { stack[stack.lastIndex] = r }

    fun reset(r: Route) { stack.clear(); stack.add(r) }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
        else if (route is Route.Main && mainTab != 0) mainTab = 0
    }

    /** Call: gems kaafi hain to call screen, warna thoda "Connecting" dikha ke recharge sheet. */
    fun startCall(hostId: String, fromMatch: Boolean = false, audio: Boolean = false) {
        val h = host(hostId) ?: return
        onlineSuggest = null
        if (Store.gems.intValue >= (if (audio) h.audioRate else h.rate)) { push(Route.Call(h.id, fromMatch, audio)); return }
        connectingHost = h
        lifecycleScope.launch {
            kotlinx.coroutines.delay(1500)
            if (connectingHost == h) { connectingHost = null; recharge = "Recharge to start a ${if (audio) "voice" else "video"} call with ${h.name}" }
        }
    }

    /**
     * Demo activity: app khuli ho to thodi-thodi der me online (Virtual) hosts ke agle script messages
     * inbox me aate hain, upar chhota banner dikhta hai. Ek baar "X is online" card bhi aata hai.
     * Sab timing/content config se ("auto_message_every", "online_popup_after", host "script").
     */
    private suspend fun demoActivityLoop() {
        val cfg = RemoteConfig.current
        var suggested = false
        var elapsed = 0
        while (true) {
            kotlinx.coroutines.delay(5000); elapsed += 5
            val onMain = route is Route.Main && recharge == null && !checkin && !lowGems
            if (!onMain) continue
            if (!suggested && cfg.onlinePopupAfter > 0 && elapsed >= cfg.onlinePopupAfter) {
                suggested = true
                onlineSuggest = cfg.hosts.filter { it.online && it.id !in Store.blocked }.randomOrNull()
                continue
            }
            if (cfg.autoMessageEvery > 0 && elapsed % cfg.autoMessageEvery == 0) {
                val h = cfg.hosts.filter { it.online && it.id !in Store.blocked }.shuffled().firstOrNull { h ->
                    (h.script ?: cfg.chatScript).size > (Store.scriptPos[h.id] ?: 0)
                } ?: continue
                val m = (h.script ?: cfg.chatScript)[Store.scriptPos[h.id] ?: 0]
                Store.addMsg(h.id, ChatMsg(false, m.type, m.text, m.url, m.duration, System.currentTimeMillis()), countUnread = true)
                Store.advanceScript(h.id)
                banner = h to preview(ChatMsg(false, m.type, m.text, m.url, m.duration, 0))
                kotlinx.coroutines.delay(3500)
                banner = null
            }
        }
    }

    fun host(id: String): Host? = RemoteConfig.current.hosts.firstOrNull { it.id == id }

    /** Gems kam hain: "Gems kam" popup, jisme Recharge / Free gems. */
    fun needGems() { lowGems = true }

    fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
