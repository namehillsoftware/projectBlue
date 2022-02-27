package com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity

import com.lasthopesoftware.bluewater.shared.messages.TypedMessage

interface NowPlayingMessage : TypedMessage

class ToggleEditPlaylist(val isEditing: Boolean) : NowPlayingMessage
