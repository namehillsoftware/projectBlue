package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.content.Intent
import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.FileListActivity.Companion.startFileListActivity
import com.lasthopesoftware.bluewater.client.browsing.items.menu.ActivityLaunching
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.messages.SendApplicationMessages
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

		sendMessages.sendMessage(ActivityLaunching.LAUNCHING)
		provideItems
			.promiseItems(libraryId, item.itemId)
			.then(
				{ items ->
					if (items.isNotEmpty()) {
						val itemListIntent = Intent(context, cls<ItemListActivity>())
						itemListIntent.putExtra(ItemListActivity.KEY, item.key)
						itemListIntent.putExtra(ItemListActivity.VALUE, item.value)
						context.startActivity(itemListIntent)
					} else {
						startFileListActivity(context, item)
					}
				},
				{ e ->
					logger.error("An error occurred getting nested items for item " + item.key, e)
				}
			)
			.then(
				{ sendMessages.sendMessage(ActivityLaunching.FINISHED) },
				{ sendMessages.sendMessage(ActivityLaunching.HALTED) })
	}
}
