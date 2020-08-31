package com.lasthopesoftware.bluewater.client.playback.view.nowplaying.list

import android.content.Context
import android.view.ViewGroup
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFileDiffer
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.menu.NowPlayingFileListItemMenuBuilder
import com.lasthopesoftware.bluewater.shared.android.adapters.DeferredListAdapter

class NowPlayingFileListAdapter(
	context: Context,
	private val nowPlayingFileListItemMenuBuilder: NowPlayingFileListItemMenuBuilder)
	: DeferredListAdapter<PositionedFile, NowPlayingFileListItemMenuBuilder.ViewHolder>(context, PositionedFileDiffer) {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = nowPlayingFileListItemMenuBuilder.newViewHolder(parent)

	override fun onBindViewHolder(holder: NowPlayingFileListItemMenuBuilder.ViewHolder, position: Int) =
		holder.update(getItem(position))
}
