package com.myfuturecode.chatsetu

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class ChatMsg(val mine: Boolean, val type: String, val text: String, val url: String, val duration: Int, val time: Long)

data class Purchase(val orderId: String, val packId: String, val gems: Int, val price: String, val mode: String, val status: String, val time: Long)

/** User ka local data: profile, gems, check-in, chats, purchases. (Baad me server pe shift kar sakte ho.) */
object Store {
    private lateinit var sp: SharedPreferences

    val name = mutableStateOf("")
    val dob = mutableStateOf("")
    val gender = mutableStateOf("")
    val interests = mutableStateListOf<String>()
    val userId = mutableStateOf("")
    val gems = mutableIntStateOf(0)
    val checkinDay = mutableIntStateOf(0)        // kitne din lagatar claim hue (0..7)
    val lastCheckin = mutableStateOf("")
    val blocked = mutableStateListOf<String>()
    val chats = mutableStateMapOf<String, List<ChatMsg>>()
    val unread = mutableStateMapOf<String, Int>()
    val scriptPos = mutableStateMapOf<String, Int>()
    val purchases = mutableStateListOf<Purchase>()
    val matches = mutableIntStateOf(0)

    val onboarded get() = name.value.isNotBlank()

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("store", 0)
        name.value = sp.getString("name", "")!!
        dob.value = sp.getString("dob", "")!!
        gender.value = sp.getString("gender", "")!!
        interests.addAll(sp.getString("interests", "")!!.split(',').filter { it.isNotBlank() })
        userId.value = sp.getString("uid", null) ?: (1_000_000 + (Math.random() * 8_999_999).toInt()).toString().also { sp.edit().putString("uid", it).apply() }
        gems.intValue = sp.getInt("gems", 0)
        checkinDay.intValue = sp.getInt("checkin_day", 0)
        lastCheckin.value = sp.getString("last_checkin", "")!!
        matches.intValue = sp.getInt("matches", 0)
        blocked.addAll(sp.getString("blocked", "")!!.split(',').filter { it.isNotBlank() })
        runCatching {
            val o = JSONObject(sp.getString("chats", "{}")!!)
            o.keys().forEach { k ->
                val a = o.getJSONArray(k)
                chats[k] = (0 until a.length()).map { i -> a.getJSONObject(i).let { m ->
                    ChatMsg(m.optBoolean("m"), m.optString("ty"), m.optString("t"), m.optString("u"), m.optInt("d"), m.optLong("at"))
                } }
            }
            val u = JSONObject(sp.getString("unread", "{}")!!); u.keys().forEach { unread[it] = u.optInt(it) }
            val s = JSONObject(sp.getString("script", "{}")!!); s.keys().forEach { scriptPos[it] = s.optInt(it) }
            val p = JSONArray(sp.getString("purchases", "[]")!!)
            (0 until p.length()).forEach { i -> p.getJSONObject(i).let { o2 ->
                purchases.add(Purchase(o2.optString("o"), o2.optString("p"), o2.optInt("g"), o2.optString("pr"), o2.optString("mo"), o2.optString("s"), o2.optLong("t")))
            } }
        }
    }

    fun saveProfile(n: String, d: String, g: String, tags: List<String>) {
        name.value = n; dob.value = d; gender.value = g
        interests.clear(); interests.addAll(tags)
        val first = !sp.getBoolean("welcomed", false)
        sp.edit().putString("name", n).putString("dob", d).putString("gender", g)
            .putString("interests", tags.joinToString(",")).putBoolean("welcomed", true).apply()
        if (first) addGems(RemoteConfig.current.welcomeGems)
    }

    fun addGems(n: Int) { gems.intValue += n; sp.edit().putInt("gems", gems.intValue).apply() }

    /** Gems kaafi hain to kaat do aur true, warna false (UI recharge dikhaye). */
    fun spend(n: Int): Boolean {
        if (gems.intValue < n) return false
        addGems(-n); return true
    }

    fun addMatch() { matches.intValue++; sp.edit().putInt("matches", matches.intValue).apply() }

    // ----- daily check-in -----
    private fun today() = LocalDate.now().toString()

    val canCheckin get() = lastCheckin.value != today()

    /** Aaj ka din (0-based) jo claim hoga. Ek din miss hua to streak 0 se. */
    val checkinIndex: Int
        get() {
            val yesterday = LocalDate.now().minusDays(1).toString()
            val streak = if (lastCheckin.value == yesterday || lastCheckin.value == today()) checkinDay.intValue else 0
            return if (canCheckin) streak % 7 else (streak - 1).coerceAtLeast(0) % 7
        }

    fun claimCheckin(): Int {
        if (!canCheckin) return 0
        val idx = checkinIndex
        val reward = RemoteConfig.current.checkin[idx]
        checkinDay.intValue = idx + 1
        lastCheckin.value = today()
        sp.edit().putInt("checkin_day", checkinDay.intValue).putString("last_checkin", lastCheckin.value).apply()
        addGems(reward)
        return reward
    }

    // ----- chats -----
    fun addMsg(hostId: String, m: ChatMsg, countUnread: Boolean) {
        chats[hostId] = (chats[hostId] ?: emptyList()) + m
        if (countUnread) unread[hostId] = (unread[hostId] ?: 0) + 1
        saveChats()
    }

    fun markRead(hostId: String) { if ((unread[hostId] ?: 0) > 0) { unread[hostId] = 0; saveChats() } }

    fun advanceScript(hostId: String) { scriptPos[hostId] = (scriptPos[hostId] ?: 0) + 1; saveChats() }

    private fun saveChats() {
        val o = JSONObject()
        chats.forEach { (k, list) ->
            o.put(k, JSONArray().apply { list.takeLast(200).forEach { m ->
                put(JSONObject().put("m", m.mine).put("ty", m.type).put("t", m.text).put("u", m.url).put("d", m.duration).put("at", m.time))
            } })
        }
        sp.edit().putString("chats", o.toString())
            .putString("unread", JSONObject(unread.toMap() as Map<*, *>).toString())
            .putString("script", JSONObject(scriptPos.toMap() as Map<*, *>).toString()).apply()
    }

    // ----- block / report -----
    fun block(hostId: String) {
        if (hostId !in blocked) blocked.add(hostId)
        sp.edit().putString("blocked", blocked.joinToString(",")).apply()
    }

    fun unblock(hostId: String) { blocked.remove(hostId); sp.edit().putString("blocked", blocked.joinToString(",")).apply() }

    // ----- purchases -----
    fun addPurchase(p: Purchase) {
        purchases.removeAll { it.orderId == p.orderId }
        purchases.add(0, p)
        val a = JSONArray()
        purchases.take(100).forEach { a.put(JSONObject().put("o", it.orderId).put("p", it.packId).put("g", it.gems).put("pr", it.price).put("mo", it.mode).put("s", it.status).put("t", it.time)) }
        sp.edit().putString("purchases", a.toString()).apply()
    }

    /** Account delete: saara local data saaf. */
    fun deleteAll(ctx: Context) {
        sp.edit().clear().apply()
        ctx.getSharedPreferences("remote_config", 0).edit().clear().apply()
        name.value = ""; dob.value = ""; gender.value = ""; interests.clear(); gems.intValue = 0
        checkinDay.intValue = 0; lastCheckin.value = ""; blocked.clear(); chats.clear(); unread.clear(); scriptPos.clear(); purchases.clear(); matches.intValue = 0
    }
}
