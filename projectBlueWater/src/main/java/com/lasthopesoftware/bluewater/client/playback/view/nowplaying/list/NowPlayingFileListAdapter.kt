package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.list

import android.view.ViewGroup
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.ViewChangedHandler
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFileDiffer
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.menu.NowPlayingFileListItemMenuBuilder
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.INowPlayingRepository
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter

class NowPlayingFileListAdapter(
	itemListMenuChangeHandler: IItemListMenuChangeHandler,
	nowPlayingRepository: INowPlayingRepository)
	: DeferredListAdapter<PositionedFile, NowPlayingFileListItemMenuBuilder.ViewHolder>(PositionedFileDiffer) {

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
		holder.update(getItem(position))
	}
}
