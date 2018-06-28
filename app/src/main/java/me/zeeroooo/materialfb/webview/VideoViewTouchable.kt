package me.zeeroooo.materialfb.webview

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView

class VideoViewTouchable : VideoView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun performClick(): Boolean {
        return super.performClick()
    }
}