package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.fragments.playlist

import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.shared.messages.TypedMessage

interface NowPlayingPlaylistMessage : TypedMessage

object EditPlaylist : NowPlayingPlaylistMessage
object FinishEditPlaylist : NowPlayingPlaylistMessage

class DragItem(val positionedFile: PositionedFile) : NowPlayingPlaylistMessage
