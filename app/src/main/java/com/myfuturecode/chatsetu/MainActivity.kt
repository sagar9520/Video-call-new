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
    data class Call(val hostId: String, val fromMatch: Boolean = false) : Route()
    data object Purchases : Route()
    data object Blocked : Route()
}

class MainActivity : ComponentActivity() {
    val stack = mutableStateListOf<Route>(Route.Splash)
    val route get() = stack.last()

    // Poori app ke upar khulne wale popups
    var recharge by mutableStateOf<String?>(null)      // null = band; text = upar ka message
    var lowGems by mutableStateOf(false)
    var checkin by mutableStateOf(false)
    var mainTab by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                        is Route.Call -> CallScreen(this@MainActivity, r.hostId, r.fromMatch)
                        Route.Purchases -> PurchasesScreen(this@MainActivity)
                        Route.Blocked -> BlockedScreen(this@MainActivity)
                    }
                }
                LaunchedEffect(route) {
                    if (route is Route.Main && Store.canCheckin && Store.onboarded) checkin = true
                }
                Overlays(this@MainActivity)
            }
        }
    }

    override fun onResume() {
        super.onResume()
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

    fun host(id: String): Host? = RemoteConfig.current.hosts.firstOrNull { it.id == id }

    /** Gems kam hain: "Gems kam" popup, jisme Recharge / Free gems. */
    fun needGems() { lowGems = true }

    fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
