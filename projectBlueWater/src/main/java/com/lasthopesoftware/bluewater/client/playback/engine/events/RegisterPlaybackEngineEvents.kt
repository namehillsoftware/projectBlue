package com.lasthopesoftware.bluewater.client.playback.engine.events

interface RegisterPlaybackEngineEvents {
	fun setOnPlayingFileChanged(onPlayingFileChanged: OnPlayingFileChanged?): RegisterPlaybackEngineEvents
	fun setOnPlaylistError(onPlaylistError: OnPlaylistError?): RegisterPlaybackEngineEvents
	fun setOnPlaybackStarted(onPlaybackStarted: OnPlaybackStarted?): RegisterPlaybackEngineEvents
	fun setOnPlaybackPaused(onPlaybackPaused: OnPlaybackPaused?): RegisterPlaybackEngineEvents
	fun setOnPlaybackCompleted(onPlaybackCompleted: OnPlaybackCompleted?): RegisterPlaybackEngineEvents
	fun setOnPlaylistReset(onPlaylistReset: OnPlaylistReset?): RegisterPlaybackEngineEvents
}
