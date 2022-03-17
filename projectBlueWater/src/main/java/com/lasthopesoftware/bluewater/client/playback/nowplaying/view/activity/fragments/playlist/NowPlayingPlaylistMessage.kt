package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.menu.NowPlayingFileListItemMenuBuilder
import com.lasthopesoftware.bluewater.shared.messages.TypedMessage

interface NowPlayingPlaylistMessage : TypedMessage

object EditPlaylist : NowPlayingPlaylistMessage
object FinishEditPlaylist : NowPlayingPlaylistMessage

class ItemDragged(val positionedFile: PositionedFile, val viewHolder: NowPlayingFileListItemMenuBuilder.ViewHolder)
	: NowPlayingPlaylistMessage
