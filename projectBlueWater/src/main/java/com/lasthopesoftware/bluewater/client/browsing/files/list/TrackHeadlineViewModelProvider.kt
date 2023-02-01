package com.lasthopesoftware.bluewater.client.browsing.files.list

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKey
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.resources.strings.GetStringResources
import java.util.concurrent.ConcurrentLinkedQueue

class TrackHeadlineViewModelProvider(
	private val filePropertiesProvider: ProvideScopedFileProperties,
	private val urlKeyProvider: ProvideScopedUrlKey,
	private val stringResources: GetStringResources,
	private val controlPlaybackService: ControlPlaybackService,
	private val applicationNavigation: NavigateApplication,
	private val sendItemMenuMessages: SendTypedMessages<ItemListMenuMessage>,
	private val receiveApplicationMessages: RegisterForApplicationMessages,
) : ViewModel() {

	private val allViewModels = ConcurrentLinkedQueue<PooledFileItemViewModel>()
	private val viewModelPool = ConcurrentLinkedQueue<PooledFileItemViewModel>()

	override fun onCleared() {
		allViewModels.forEach { it.close() }
		super.onCleared()
	}

	fun getViewModel(): ViewFileItem = viewModelPool.poll()
		?: PooledFileItemViewModel(
			ReusableTrackHeadlineViewModel(
				filePropertiesProvider,
				urlKeyProvider,
				stringResources,
				controlPlaybackService,
				applicationNavigation,
				sendItemMenuMessages,
				receiveApplicationMessages,
			)
		).also(allViewModels::offer)

	private inner class PooledFileItemViewModel(private val inner: ReusableTrackHeadlineViewModel) : ViewFileItem by inner, AutoCloseable {
		override fun reset() {
			inner.reset()
			viewModelPool.offer(this)
		}

		override fun close() {
			inner.close()
			viewModelPool.offer(this)
		}
	}
}
