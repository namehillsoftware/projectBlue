package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers

import android.widget.ViewAnimator
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.OnAllMenusHidden
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.OnAnyMenuShown
import com.lasthopesoftware.bluewater.client.browsing.items.menu.LongClickViewAnimatorListener
import com.lasthopesoftware.bluewater.client.browsing.items.menu.OnViewChangedListener

class ViewChangedHandler : OnViewChangedListener {
	private var shownMenu: ViewAnimator? = null
	private var onViewChangedListener: OnViewChangedListener? = null
	private var onAnyMenuShown: OnAnyMenuShown? = null
	private var onAllMenusHidden: OnAllMenusHidden? = null
	private var numberOfMenusShown = 0

	override fun onViewChanged(viewAnimator: ViewAnimator) {
		if (viewAnimator.displayedChild > 0) {
			if (numberOfMenusShown == 0) onAnyMenuShown?.onAnyMenuShown()
			++numberOfMenusShown

			shownMenu?.also { LongClickViewAnimatorListener.tryFlipToPreviousView(it) }
			shownMenu = viewAnimator
		} else {
			if (shownMenu === viewAnimator) shownMenu = null
			if (--numberOfMenusShown == 0) onAllMenusHidden?.onAllMenusHidden()
		}

		onViewChangedListener?.onViewChanged(viewAnimator)
	}

	fun setOnViewChangedListener(onViewChangedListener: OnViewChangedListener?): ViewChangedHandler {
		this.onViewChangedListener = onViewChangedListener
		return this
	}

	fun setOnAnyMenuShown(onAnyMenuShown: OnAnyMenuShown?): ViewChangedHandler {
		this.onAnyMenuShown = onAnyMenuShown
		return this
	}

	fun setOnAllMenusHidden(onAllMenusHidden: OnAllMenusHidden?): ViewChangedHandler {
		this.onAllMenusHidden = onAllMenusHidden
		return this
	}
}
