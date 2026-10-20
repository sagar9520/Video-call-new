package com.myfuturecode.chatsetu

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Host(
    val id: String, val name: String, val age: Int, val city: String, val lang: String, val bio: String,
    val tags: List<String>, val photos: List<String>, val video: String, val online: Boolean, val rate: Int,
    val color: String, val virtual: Boolean, val script: List<ScriptMsg>?,
)

/** Host ki taraf se aane wala message: text / image / voice / video. */
data class ScriptMsg(val type: String, val text: String, val url: String, val duration: Int, val delay: Int)

data class Gift(val id: String, val name: String, val emoji: String, val icon: String, val price: Int, val category: String)

data class Pack(val id: String, val gems: Int, val price: String, val off: String, val cashfreeUrl: String)

data class AdUnits(val appOpen: String, val banner: String, val interstitial: String, val rewarded: String)

data class AdsConfig(
    val enabled: Boolean, val network: String, val interstitialEvery: Int, val appOpen: Boolean,
    val admob: AdUnits, val max: AdUnits,
)

data class PromoApp(val name: String, val pkg: String, val icon: String, val desc: String)

data class AppConfig(
    val virtualLabel: String,
    val welcomeGems: Int,
    val msgCost: Int,
    val filterCost: Int,
    val rewardedGems: Int,
    val checkin: List<Int>,
    val paymentMode: String,          // play | cashfree | both
    val verifyUrl: String,
    val privacyUrl: String,
    val hosts: List<Host>,
    val chatScript: List<ScriptMsg>,
    val gifts: List<Gift>,
    val packs: List<Pack>,
    val ads: AdsConfig,
    val crossPromo: List<PromoApp>,
)

/**
 * Config pehle cache/asset se turant load hota hai, phir server (get_json.php?file=<package>) se refresh.
 * Hosts, unki photos/videos, chat ke messages, gifts, packs sab yahin se badalte hain.
 */
object RemoteConfig {
    private const val PREF = "remote_config"
    private val _config = MutableStateFlow<AppConfig?>(null)
    val config: StateFlow<AppConfig?> = _config
    val current: AppConfig get() = _config.value!!

    fun loadLocal(ctx: Context) {
        val cached = ctx.getSharedPreferences(PREF, 0).getString("json", null)
        val asset = ctx.assets.open("default_config.json").bufferedReader().use { it.readText() }
        _config.value = runCatching { parse(JSONObject(cached!!)) }.getOrNull() ?: parse(JSONObject(asset))
    }

    suspend fun refresh(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        if (BuildConfig.CONFIG_URL.isBlank()) return@withContext false
        runCatching {
            val client = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS).build()
            val req = Request.Builder()
                .url("${BuildConfig.CONFIG_URL}?file=${ctx.packageName}")
                .header("X-App-Key", BuildConfig.CONFIG_KEY)
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use false
                val root = JSONObject(res.body!!.string())
                val data = root.optJSONObject("data") ?: return@use false
                if (!data.has("hosts")) return@use false
                if (!data.has("cross_promo") && root.has("cross_promo")) data.put("cross_promo", root.get("cross_promo"))
                val parsed = parse(data)
                ctx.getSharedPreferences(PREF, 0).edit().putString("json", data.toString()).apply()
                _config.value = parsed
                true
            }
        }.getOrDefault(false)
    }

    private inline fun <T> JSONArray?.mapObj(f: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it)?.let(f) }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }

    private fun units(o: JSONObject?) = AdUnits(
        o?.optString("app_open").orEmpty(), o?.optString("banner").orEmpty(),
        o?.optString("interstitial").orEmpty(), o?.optString("rewarded").orEmpty(),
    )

    private fun script(a: JSONArray?) = a.mapObj {
        ScriptMsg(it.optString("type", "text"), it.optString("text"), it.optString("url"), it.optInt("duration", 10), it.optInt("delay", 3))
    }

    fun parse(o: JSONObject): AppConfig {
        val ads = o.optJSONObject("ads")
        val checkin = o.optJSONArray("checkin")?.let { a -> (0 until a.length()).map { a.optInt(it) } }
        return AppConfig(
            virtualLabel = o.optString("virtual_label", "Virtual"),
            welcomeGems = o.optInt("welcome_gems", 50),
            msgCost = o.optInt("msg_cost", 1),
            filterCost = o.optInt("filter_cost", 5),
            rewardedGems = o.optInt("rewarded_gems", 5),
            checkin = checkin?.takeIf { it.size == 7 } ?: listOf(50, 80, 50, 50, 50, 50, 100),
            paymentMode = o.optString("payment_mode", "play"),
            verifyUrl = o.optString("verify_url"),
            privacyUrl = o.optString("privacy_url"),
            hosts = o.optJSONArray("hosts").mapObj {
                Host(
                    it.optString("id"), it.optString("name"), it.optInt("age", 22), it.optString("city"), it.optString("lang"),
                    it.optString("bio"), it.optJSONArray("tags").strings(), it.optJSONArray("photos").strings(), it.optString("video"),
                    it.optBoolean("online", true), it.optInt("rate", 60), it.optString("color", "#9C02BA"),
                    // Jab tak real host nahi, "virtual": true rehta hai aur UI me label dikhta hai
                    it.optBoolean("virtual", true),
                    it.optJSONArray("script")?.let { s -> script(s) },
                )
            }.filter { it.id.isNotBlank() },
            chatScript = script(o.optJSONArray("chat_script")),
            gifts = o.optJSONArray("gifts").mapObj {
                Gift(it.optString("id"), it.optString("name"), it.optString("emoji", "🎁"), it.optString("icon"), it.optInt("price", 10), it.optString("category", "Hot"))
            },
            packs = o.optJSONArray("packs").mapObj {
                Pack(it.optString("id"), it.optInt("gems"), it.optString("price"), it.optString("off"), it.optString("cashfree_url"))
            },
            ads = AdsConfig(
                enabled = ads?.optBoolean("enabled", true) ?: false,
                network = ads?.optString("network", "admob") ?: "admob",
                interstitialEvery = ads?.optInt("interstitial_every", 3) ?: 3,
                appOpen = ads?.optBoolean("app_open", true) ?: false,
                admob = units(ads?.optJSONObject("admob")),
                max = units(ads?.optJSONObject("max")),
            ),
            crossPromo = o.optJSONArray("cross_promo").mapObj {
                PromoApp(it.optString("name", it.optString("title")), it.optString("package", it.optString("pkg")),
                    it.optString("icon"), it.optString("desc", it.optString("description")))
            },
        )
    }
}
