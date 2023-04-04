package com.lasthopesoftware.bluewater.client.browsing.items.list

import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages

class ReusableChildItemViewModelProvider(
	private val storedItemAccess: AccessStoredItems,
	private val sendItemMenuMessages: SendTypedMessages<ItemListMenuMessage>,
) : PooledCloseablesViewModel<ReusableChildItemViewModel>() {
	override fun getNewCloseable(): ReusableChildItemViewModel = ReusableChildItemViewModel(
		storedItemAccess,
		sendItemMenuMessages,
	)
}
