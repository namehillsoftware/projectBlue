package com.lasthopesoftware.bluewater.client.browsing.files.list

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.details.LaunchFileDetails
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKeyProvider
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.resources.strings.GetStringResources
import java.util.concurrent.ConcurrentLinkedQueue

class TrackHeadlineViewModelProvider(
	private val filePropertiesProvider: ProvideScopedFileProperties,
	private val urlKeyProvider: ProvideScopedUrlKeyProvider,
	private val stringResources: GetStringResources,
	private val controlPlaybackService: ControlPlaybackService,
	private val fileDetailsLauncher: LaunchFileDetails,
	private val sendItemMenuMessages: SendTypedMessages<ItemListMenuMessage>,
	private val receiveApplicationMessages: RegisterForApplicationMessages,
) : ViewModel() {

	private val allViewModels = ConcurrentLinkedQueue<ViewFileItem>()
	private val viewModelPool = ConcurrentLinkedQueue<ViewFileItem>()

	override fun onCleared() {
		allViewModels.forEach { it.reset() }
		super.onCleared()
	}

	fun getViewModel(): ViewFileItem = viewModelPool.poll()
		?: PooledFileItemViewModel(
			ReusableTrackHeadlineViewModel(
				filePropertiesProvider,
				urlKeyProvider,
				stringResources,
				controlPlaybackService,
				fileDetailsLauncher,
				sendItemMenuMessages,
				receiveApplicationMessages,
			)
		).also(allViewModels::offer)

	private inner class PooledFileItemViewModel(private val inner: ViewFileItem) : ViewFileItem by inner {
		override fun reset() {
			inner.reset()
			viewModelPool.offer(this)
		}
	}
}
