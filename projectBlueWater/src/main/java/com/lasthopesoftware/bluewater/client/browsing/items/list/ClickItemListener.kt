package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.content.Intent
import android.view.View
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.FileListActivity.Companion.startFileListActivity
import com.lasthopesoftware.bluewater.client.browsing.items.menu.Notifications
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages
import org.slf4j.LoggerFactory

class ClickItemListener(
	private val item: Item,
	private val provideItems: ProvideItems,
	private val sendMessages: SendMessages
) : View.OnClickListener {

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(ClickItemListener::class.java) }
	}

	override fun onClick(view: View?) {
		val context = view?.context ?: return

		sendMessages.sendBroadcast(Intent(Notifications.launchingActivity))
		provideItems
			.promiseItems(item.key)
			.then(
				{ items ->
					if (items.isNotEmpty()) {
						val itemListIntent = Intent(context, ItemListActivity::class.java)
						itemListIntent.putExtra(ItemListActivity.KEY, item.key)
						itemListIntent.putExtra(ItemListActivity.VALUE, item.value)
						context.startActivity(itemListIntent)
					} else {
						startFileListActivity(context, item)
					}
				},
				{ e ->
					logger.error(
						"An error occurred getting nested items for item " + item.key,
						e
					)
				}
			)
			.must {
				sendMessages.sendBroadcast(Intent(Notifications.launchingActivityFinished))
			}
	}
}
