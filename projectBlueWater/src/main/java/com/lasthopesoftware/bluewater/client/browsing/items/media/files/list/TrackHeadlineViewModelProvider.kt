package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.details.LaunchFileDetails
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.resources.strings.GetStringResources
import java.util.concurrent.ConcurrentLinkedQueue

class TrackHeadlineViewModelProvider(
	private val filePropertiesProvider: ProvideScopedFileProperties,
	private val stringResources: GetStringResources,
	private val controlPlaybackService: ControlPlaybackService,
	private val fileDetailsLauncher: LaunchFileDetails,
	private val sendItemMenuMessages: SendTypedMessages<ItemListMenuMessage>,
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
				stringResources,
				controlPlaybackService,
				fileDetailsLauncher,
				sendItemMenuMessages,
			)
		).also(allViewModels::offer)

	private inner class PooledFileItemViewModel(private val inner: ViewFileItem) : ViewFileItem by inner {
		override fun reset() {
			inner.reset()
			viewModelPool.offer(this)
		}
	}
}
