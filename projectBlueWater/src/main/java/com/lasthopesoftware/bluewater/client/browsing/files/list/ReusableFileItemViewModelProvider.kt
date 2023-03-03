package com.lasthopesoftware.bluewater.client.browsing.files.list

import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKey
import com.lasthopesoftware.bluewater.shared.android.viewmodels.PooledCloseablesViewModel
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.resources.strings.GetStringResources

class ReusableFileItemViewModelProvider(
	private val filePropertiesProvider: ProvideScopedFileProperties,
	private val urlKeyProvider: ProvideScopedUrlKey,
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
