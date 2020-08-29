package com.lasthopesoftware.bluewater.client.browsing.items.menu

abstract class AbstractListItemViewChangedListener {
	private var onViewChangedListener: OnViewChangedListener? = null

	protected fun getOnViewChangedListener(): OnViewChangedListener? = onViewChangedListener

	fun setOnViewChangedListener(onViewChangedListener: OnViewChangedListener?) {
		this.onViewChangedListener = onViewChangedListener
	}
}
