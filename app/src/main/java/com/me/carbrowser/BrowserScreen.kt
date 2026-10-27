package com.me.carbrowser
import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Bundle
import android.os.SystemClock
import android.view.Display
import android.view.MotionEvent
import android.webkit.*
import androidx.car.app.*
import androidx.car.app.model.*
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

private val BLOCK = listOf("doubleclick.net", "googlesyndication.com", "googleadservices.com",
    "adservice.google.", "adsystem.com", "taboola.com", "outbrain.com", "popads.net",
    "propellerads.com", "adnxs.com", "moatads.com", "scorecardresearch.com", "/pagead/", "/ads/")

class WebPresentation(ctx: Context, d: Display) : Presentation(ctx, d) {
    lateinit var web: WebView
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        web = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? {
                    val u = r.url.toString()
                    return if (BLOCK.any { u.contains(it) })
                        WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0))) else null
                }
            }
            loadUrl(Bus.lastUrl)
        }
        setContentView(web)
    }
}

class BrowserScreen(ctx: CarContext) : Screen(ctx), SurfaceCallback {
    private var vd: VirtualDisplay? = null
    private var pres: WebPresentation? = null

    init {
        ctx.getCarService(AppManager::class.java).setSurfaceCallback(this)
        Bus.listener = { url -> pres?.web?.post { pres?.web?.loadUrl(url) } }
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { release(); Bus.listener = null }
        })
    }

    private fun release() { pres?.dismiss(); pres = null; vd?.release(); vd = null }

    override fun onSurfaceAvailable(c: SurfaceContainer) {
        release()
        val dm = carContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        vd = dm.createVirtualDisplay("carbrowser", c.width, c.height, c.dpi, c.surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION)
        pres = WebPresentation(carContext, vd!!.display).also { it.show() }
    }

    override fun onSurfaceDestroyed(c: SurfaceContainer) { release() }

    override fun onClick(x: Float, y: Float) {
        val w = pres?.web ?: return
        val t = SystemClock.uptimeMillis()
        for ((a, tm) in listOf(MotionEvent.ACTION_DOWN to t, MotionEvent.ACTION_UP to t + 50)) {
            val e = MotionEvent.obtain(t, tm, a, x, y, 0); w.dispatchTouchEvent(e); e.recycle()
        }
    }
    override fun onScroll(dx: Float, dy: Float) { pres?.web?.scrollBy(dx.toInt(), dy.toInt()) }

    override fun onGetTemplate(): Template {
        fun act(title: String, f: () -> Unit) = Action.Builder().setTitle(title).setOnClickListener { f() }.build()
        return NavigationTemplate.Builder().setActionStrip(
            ActionStrip.Builder()
                .addAction(act("Back") { pres?.web?.let { if (it.canGoBack()) it.goBack() } })
                .addAction(act("Reload") { pres?.web?.reload() })
                .addAction(act("Home") { pres?.web?.loadUrl("https://www.google.com") })
                .build()).build()
    }
}
