package com.lasthopesoftware.bluewater.client.browsing.files.list

import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideLibraryFileProperties
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.resources.strings.GetStringResources

class ReusableFileItemViewModelProvider(
	private val filePropertiesProvider: ProvideLibraryFileProperties,
	private val urlKeyProvider: ProvideUrlKey,
	private val stringResources: GetStringResources,
	private val receiveApplicationMessages: RegisterForApplicationMessages,
) : PooledCloseablesViewModel<ViewFileItem>() {
	override fun getNewCloseable() = ReusableFileViewModel(
		filePropertiesProvider,
		stringResources,
		urlKeyProvider,
		receiveApplicationMessages
	)
}
