package com.myfuturecode.chatsetu

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase as PlayPurchase
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Gems kharidne ke 2 raaste (config "payment_mode"):
 *  - play:     Google Play Billing (consumable in-app products, product id = pack id)
 *  - cashfree: pack ka Cashfree payment link browser me; gems tab milte hain jab "verify_url" paid bole
 *  - both:     dono button (India User Choice Billing ke liye)
 */
object Billing {
    private var client: BillingClient? = null
    private val details = mutableMapOf<String, ProductDetails>()
    var onMessage: (String) -> Unit = {}

    fun init(ctx: Context) {
        val c = BillingClient.newBuilder(ctx.applicationContext)
            .setListener { result, purchases -> onPurchases(result, purchases) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    // Pichhli baar ki unconsumed purchases (app band ho gaya tha) ko poora karo
                    c.queryPurchasesAsync(
                        com.android.billingclient.api.QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                    ) { r, list -> onPurchases(r, list) }
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun queryProducts() {
        val packs = RemoteConfig.current.packs
        if (packs.isEmpty()) return
        val params = QueryProductDetailsParams.newBuilder().setProductList(packs.map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it.id).setProductType(BillingClient.ProductType.INAPP).build()
        }).build()
        client?.queryProductDetailsAsync(params) { _, list -> list.forEach { details[it.productId] = it } }
    }

    fun playPrice(pack: Pack): String? = details[pack.id]?.oneTimePurchaseOfferDetails?.formattedPrice

    fun buyPlay(activity: Activity, pack: Pack) {
        val pd = details[pack.id]
        if (pd == null) {
            queryProducts()
            onMessage("This pack is not available on Google Play yet")
            return
        }
        val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd).build()),
        ).build()
        client?.launchBillingFlow(activity, flow)
    }

    private fun onPurchases(result: BillingResult, purchases: List<PlayPurchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) {
            if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED && result.responseCode != BillingClient.BillingResponseCode.OK)
                onMessage("Payment failed (${result.responseCode})")
            return
        }
        purchases.filter { it.purchaseState == PlayPurchase.PurchaseState.PURCHASED }.forEach { p ->
            val packId = p.products.firstOrNull() ?: return@forEach
            val pack = RemoteConfig.current.packs.firstOrNull { it.id == packId } ?: return@forEach
            client?.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { r, _ ->
                if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                    Store.addGems(pack.gems)
                    Store.addPurchase(Purchase(p.orderId ?: p.purchaseToken.take(16), pack.id, pack.gems, pack.price, "Google Play", "Paid", System.currentTimeMillis()))
                    onMessage("+${pack.gems} gems added")
                }
            }
        }
    }

    // ---------- Cashfree link ----------
    fun buyCashfree(ctx: Context, pack: Pack) {
        if (pack.cashfreeUrl.isBlank()) { onMessage("Payment link not set for this pack"); return }
        val order = "CS" + UUID.randomUUID().toString().replace("-", "").take(14).uppercase()
        Store.addPurchase(Purchase(order, pack.id, pack.gems, pack.price, "Cashfree", "Pending", System.currentTimeMillis()))
        val url = Uri.parse(pack.cashfreeUrl).buildUpon()
            .appendQueryParameter("order_id", order)
            .appendQueryParameter("user_id", Store.userId.value)
            .build()
        ctx.startActivity(Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** verify_url?order_id=..&user_id=..&pack=..  ->  {"status":"paid"} ho to gems credit. */
    suspend fun verifyPending() = withContext(Dispatchers.IO) {
        val base = RemoteConfig.current.verifyUrl
        if (base.isBlank()) return@withContext
        val http = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS).build()
        Store.purchases.filter { it.status == "Pending" }.forEach { p ->
            runCatching {
                val u = Uri.parse(base).buildUpon().appendQueryParameter("order_id", p.orderId)
                    .appendQueryParameter("user_id", Store.userId.value).appendQueryParameter("pack", p.packId).build().toString()
                http.newCall(Request.Builder().url(u).header("X-App-Key", BuildConfig.CONFIG_KEY).build()).execute().use { res ->
                    val status = JSONObject(res.body!!.string()).optString("status").lowercase()
                    withContext(Dispatchers.Main) {
                        if (status == "paid") {
                            Store.addGems(p.gems); Store.addPurchase(p.copy(status = "Paid")); onMessage("+${p.gems} gems added")
                        } else if (status == "failed") Store.addPurchase(p.copy(status = "Failed"))
                    }
                }
            }
        }
    }
}
