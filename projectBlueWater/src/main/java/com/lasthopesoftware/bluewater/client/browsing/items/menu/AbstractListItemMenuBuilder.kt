package com.lasthopesoftware.bluewater.client.browsing.items.menu

import android.view.View
import android.view.ViewGroup

abstract class AbstractListItemMenuBuilder<T> {
	var onViewChangedListener: OnViewChangedListener? = null

	abstract fun getView(position: Int, item: T, convertView: View?, parent: ViewGroup): View
}
