package com.lasthopesoftware.bluewater.client.browsing.items.menu

import android.view.View
import android.view.View.OnLongClickListener
import android.widget.ViewAnimator

class LongClickViewAnimatorListener : OnLongClickListener {
	override fun onLongClick(view: View): Boolean {
		if (view is ViewAnimator) {
			view.showNext()
			return true
		}
		return false
	}

	companion object {
		fun tryFlipToPreviousView(viewAnimator: ViewAnimator?): Boolean {
			if (viewAnimator == null || viewAnimator.displayedChild == 0) return false
			viewAnimator.showPrevious()
			return true
		}
	}
}
