package com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.list

import android.content.Context
import android.view.ViewGroup
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFileDiffer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.menu.NowPlayingFileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter

class NowPlayingFileListAdapter(
	context: Context,
	itemListMenuChangeHandler: IItemListMenuChangeHandler,
	nowPlayingRepository: INowPlayingRepository)
	: DeferredListAdapter<ServiceFile, NowPlayingFileListItemMenuBuilder.ViewHolder>(context, ServiceFileDiffer) {

	private val nowPlayingFileListItemMenuBuilder = NowPlayingFileListItemMenuBuilder(nowPlayingRepository)

	init {
		val viewChangedHandler = ViewChangedHandler()
		viewChangedHandler.setOnAllMenusHidden(itemListMenuChangeHandler)
		viewChangedHandler.setOnAnyMenuShown(itemListMenuChangeHandler)
		viewChangedHandler.setOnViewChangedListener(itemListMenuChangeHandler)

		nowPlayingFileListItemMenuBuilder.setOnViewChangedListener(viewChangedHandler)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NowPlayingFileListItemMenuBuilder.ViewHolder {
		return nowPlayingFileListItemMenuBuilder.newViewHolder(parent)
	}

	override fun onBindViewHolder(holder: NowPlayingFileListItemMenuBuilder.ViewHolder, position: Int) {
		holder.update(PositionedFile(position, getItem(position)))
	}
}
