package com.lasthopesoftware.bluewater.client.browsing.items.menu

import android.view.View
import android.view.View.OnLongClickListener
import android.widget.ViewAnimator

class LongClickViewAnimatorListener(private val viewAnimator: ViewAnimator) : OnLongClickListener {
	override fun onLongClick(view: View): Boolean {
		viewAnimator.showNext()
		return true
	}

	companion object {
		fun ViewAnimator.tryFlipToPreviousView(): Boolean {
			if (displayedChild == 0) return false
			showPrevious()
			return true
		}
	}
}
