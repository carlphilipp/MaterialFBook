package me.zeeroooo.materialfb.webview

import android.content.Context
import android.content.SharedPreferences
import android.support.v4.view.NestedScrollingChild
import android.support.v4.view.NestedScrollingChildHelper
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import com.github.clans.fab.FloatingActionMenu

class MFBWebView : WebView, NestedScrollingChild {

    private lateinit var mChildHelper: NestedScrollingChildHelper
    private lateinit var floatingActionMenu: FloatingActionMenu
    private var scrollThreshold: Int = 0

    private var lastY: Int = 0
    private var nestedOffsetY: Int = 0
    private val scrollOffset = IntArray(2)
    private val scrollConsumed = IntArray(2)

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        mChildHelper = NestedScrollingChildHelper(this)
        isNestedScrollingEnabled = true
    }

    fun updateSettings(preferences: SharedPreferences) {
        settings.setGeolocationEnabled(preferences.getBoolean("location_enabled", false))
        settings.minimumFontSize = Integer.parseInt(preferences.getString("textScale", "1"))
        settings.blockNetworkImage = preferences.getBoolean("stop_images", false)
        settings.setAppCacheEnabled(true)
        settings.useWideViewPort = true
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        settings.userAgentString = "Mozilla/5.0 (Linux; U; Android 2.3.6; en-us; Nexus S Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1"
    }

    override fun onScrollChanged(scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        super.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY)
        if (Math.abs(oldScrollY - scrollY) > scrollThreshold) {
            if (scrollY > oldScrollY) {
                // User scrolled down, hide the button
                floatingActionMenu.hideMenuButton(true);
            } else if (scrollY < oldScrollY) {
                // User scrolled up, show the button
                floatingActionMenu.showMenuButton(true);
            }
        }
    }

    fun setUpOnScrollChanged(floatingActionMenu: FloatingActionMenu, scrollThreshold: Int) {
        this.floatingActionMenu = floatingActionMenu
        this.scrollThreshold = scrollThreshold
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return mChildHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return mChildHelper.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        mChildHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return mChildHelper.hasNestedScrollingParent()
    }

    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?): Boolean {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var rs = false
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            nestedOffsetY = 0
        }
        val y = event.y.toInt()
        event.offsetLocation(0f, nestedOffsetY.toFloat())
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                rs = super.onTouchEvent(event)
                lastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                var dy = lastY - y
                val oldY = scrollY
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                if (dispatchNestedPreScroll(0, dy, scrollConsumed, scrollOffset)) {
                    dy -= scrollConsumed[1]
                    event.offsetLocation(0f, (-scrollOffset[1]).toFloat())
                    nestedOffsetY += scrollOffset[1]
                }
                rs = super.onTouchEvent(event)
                lastY = y - scrollOffset[1]
                if (dy < 0) {
                    val newScrollY = Math.max(0, oldY + dy)
                    dy -= newScrollY - oldY
                    if (dispatchNestedScroll(0, newScrollY - dy, 0, dy, scrollOffset)) {
                        event.offsetLocation(0f, scrollOffset[1].toFloat())
                        nestedOffsetY += scrollOffset[1]
                        lastY -= scrollOffset[1]
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                rs = super.onTouchEvent(event)
                stopNestedScroll()
            }
        }
        return rs
    }
}