package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.startItemBrowserActivity

class ClickItemListener(private val item: Item) : View.OnClickListener {

	override fun onClick(view: View?) {
		val context = view?.context ?: return

		context.startItemBrowserActivity(item)
	}
}
