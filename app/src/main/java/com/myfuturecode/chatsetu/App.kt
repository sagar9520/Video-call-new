package com.myfuturecode.chatsetu

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class App : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver, ImageLoaderFactory {
    // SVG (demo avatars) + crossfade
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(SvgDecoder.Factory()) }
        .crossfade(true)
        .build()

    private var current: Activity? = null
    private var backgroundAt = 0L

    /** Splash khatam hone ke baad hi "wapas aaye to app-open ad" chalu hota hai. */
    var splashDone = false

    override fun onCreate() {
        super<Application>.onCreate()
        Store.init(this)
        RemoteConfig.loadLocal(this)
        Ads.init(this)
        Billing.init(this)
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) { backgroundAt = System.currentTimeMillis() }

    override fun onStart(owner: LifecycleOwner) {
        val a = current ?: return
        if (splashDone && backgroundAt > 0 && System.currentTimeMillis() - backgroundAt > 30_000 && !Ads.showingFullScreen) {
            Ads.showAppOpen(a) {}
        }
    }

    override fun onActivityResumed(activity: Activity) { current = activity }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { current = activity }
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) { if (current === activity) current = null }
}
