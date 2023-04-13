package com.lasthopesoftware.bluewater.client.browsing.files.list

import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.resources.strings.GetStringResources

class ReusablePlaylistFileItemViewModelProvider(
	private val filePropertiesProvider: ProvideLibraryFileProperties,
	private val urlKeyProvider: ProvideUrlKey,
	private val stringResources: GetStringResources,
	private val sendItemMenuMessages: SendTypedMessages<ItemListMenuMessage>,
	private val receiveApplicationMessages: RegisterForApplicationMessages,
) : PooledCloseablesViewModel<ViewPlaylistFileItem>() {
	override fun getNewCloseable(): ViewPlaylistFileItem = ReusablePlaylistFileViewModel(
		sendItemMenuMessages,
		ReusableFileViewModel(
			filePropertiesProvider,
			stringResources,
			urlKeyProvider,
			receiveApplicationMessages
		)
	)
}
