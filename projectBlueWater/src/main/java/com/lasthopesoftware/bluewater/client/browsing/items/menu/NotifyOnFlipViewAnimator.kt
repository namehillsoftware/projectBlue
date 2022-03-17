package com.lasthopesoftware.bluewater.client.browsing.items.menu

import android.content.Context
import android.util.AttributeSet
import android.widget.ViewAnimator

/**
 * Created by david on 10/28/15.
 */
class NotifyOnFlipViewAnimator : ViewAnimator {
    private var onViewChangedListener: OnViewChangedListener? = null

    constructor(context: Context) : super(context)
	constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

	override fun setDisplayedChild(whichChild: Int) {
        super.setDisplayedChild(whichChild)
        onViewChangedListener?.onViewChanged(this)
    }

    fun setViewChangedListener(onViewChangedListener: OnViewChangedListener?) {
        this.onViewChangedListener = onViewChangedListener
    }
}
