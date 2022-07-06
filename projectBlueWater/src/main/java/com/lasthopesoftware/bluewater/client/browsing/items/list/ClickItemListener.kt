package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListActivity.Companion.startItemListActivity
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import org.slf4j.LoggerFactory

class ClickItemListener(
	private val libraryId: LibraryId,
	private val item: Item,
	private val provideItems: ProvideItems,
	private val sendMessages: SendApplicationMessages
) : View.OnClickListener {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(cls<ClickItemListener>()) }
	}

	override fun onClick(view: View?) {
		val context = view?.context ?: return

		context.startItemListActivity(item)
	}
}
