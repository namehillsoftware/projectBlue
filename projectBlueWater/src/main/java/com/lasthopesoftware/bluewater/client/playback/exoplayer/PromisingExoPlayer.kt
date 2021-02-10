package com.lasthopesoftware.bluewater.client.playback.exoplayer

import android.os.Looper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.namehillsoftware.handoff.promises.Promise

interface PromisingExoPlayer {
	fun getAudioComponent(): Promise<Player.AudioComponent?>

	fun getVideoComponent(): Promise<Player.VideoComponent?>

	fun getTextComponent(): Promise<Player.TextComponent?>

	fun getMetadataComponent(): Promise<Player.MetadataComponent?>

	fun getDeviceComponent(): Promise<Player.DeviceComponent?>

	fun getApplicationLooper(): Promise<Looper>

	fun addListener(listener: Player.EventListener): Promise<Unit>

	fun removeListener(listener: Player.EventListener): Promise<Unit>

	fun setMediaItems(mediaItems: MutableList<MediaItem>): Promise<Unit>

	fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean): Promise<Unit>

	fun setMediaItems(mediaItems: MutableList<MediaItem>, startWindowIndex: Int, startPositionMs: Long): Promise<Unit>

	fun setMediaItem(mediaItem: MediaItem): Promise<Unit>

	fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long): Promise<Unit>

	fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean): Promise<Unit>

	fun addMediaItem(mediaItem: MediaItem): Promise<Unit>

	fun addMediaItem(index: Int, mediaItem: MediaItem): Promise<Unit>

	fun addMediaItems(mediaItems: MutableList<MediaItem>): Promise<Unit>

	fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>): Promise<Unit>

	fun moveMediaItem(currentIndex: Int, newIndex: Int): Promise<Unit>

	fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): Promise<Unit>

	fun removeMediaItem(index: Int): Promise<Unit>

	fun removeMediaItems(fromIndex: Int, toIndex: Int): Promise<Unit>

	fun clearMediaItems(): Promise<Unit>

	fun prepare(mediaSource: MediaSource): Promise<Unit>

	fun prepare(mediaSource: MediaSource, resetPosition: Boolean, resetState: Boolean): Promise<Unit>

	fun prepare(): Promise<Unit>

	fun getPlaybackState(): Promise<Int>

	fun getPlaybackSuppressionReason(): Promise<Int>

	fun isPlaying(): Promise<Boolean>

	fun getPlayerError(): Promise<ExoPlaybackException?>

	fun getPlaybackError(): Promise<ExoPlaybackException?>

	fun play(): Promise<Unit>

	fun pause(): Promise<Unit>

	fun setPlayWhenReady(playWhenReady: Boolean): Promise<Unit>

	fun getPlayWhenReady(): Promise<Boolean>

	fun setRepeatMode(repeatMode: Int): Promise<Unit>

	fun getRepeatMode(): Promise<Int>

	fun setShuffleModeEnabled(shuffleModeEnabled: Boolean): Promise<Unit>

	fun getShuffleModeEnabled(): Promise<Boolean>

	fun isLoading(): Promise<Boolean>

	fun seekToDefaultPosition(): Promise<Unit>

	fun seekToDefaultPosition(windowIndex: Int): Promise<Unit>

	fun seekTo(positionMs: Long): Promise<Unit>

	fun seekTo(windowIndex: Int, positionMs: Long): Promise<Unit>

	fun hasPrevious(): Promise<Boolean>

	fun previous(): Promise<Unit>

	fun hasNext(): Promise<Boolean>

	fun next(): Promise<Unit>

	fun setPlaybackParameters(playbackParameters: PlaybackParameters?): Promise<Unit>

	fun getPlaybackParameters(): Promise<PlaybackParameters>

	fun stop(): Promise<Unit>

	fun stop(reset: Boolean): Promise<Unit>

	fun release(): Promise<Unit>

	fun getRendererCount(): Promise<Int>

	fun getRendererType(index: Int): Promise<Int>

	fun getTrackSelector(): Promise<TrackSelector?>

	fun getCurrentTrackGroups(): Promise<TrackGroupArray>

	fun getCurrentTrackSelections(): Promise<TrackSelectionArray>

	fun getCurrentManifest(): Promise<Any?>

	fun getCurrentTimeline(): Promise<Timeline>

	fun getCurrentPeriodIndex(): Promise<Int>

	fun getCurrentWindowIndex(): Promise<Int>

	fun getNextWindowIndex(): Promise<Int>

	fun getPreviousWindowIndex(): Promise<Int>

	fun getCurrentTag(): Promise<Any?>

	fun getCurrentMediaItem(): Promise<MediaItem?>

	fun getMediaItemCount(): Promise<Int>

	fun getMediaItemAt(index: Int): Promise<MediaItem>

	fun getDuration(): Promise<Long>

	fun getCurrentPosition(): Promise<Long>

	fun getBufferedPosition(): Promise<Long>

	fun getBufferedPercentage(): Promise<Int>

	fun getTotalBufferedDuration(): Promise<Long>

	fun isCurrentWindowDynamic(): Promise<Boolean>

	fun isCurrentWindowLive(): Promise<Boolean>

	fun getCurrentLiveOffset(): Promise<Long>

	fun isCurrentWindowSeekable(): Promise<Boolean>

	fun isPlayingAd(): Promise<Boolean>

	fun getCurrentAdGroupIndex(): Promise<Int>

	fun getCurrentAdIndexInAdGroup(): Promise<Int>

	fun getContentDuration(): Promise<Long>

	fun getContentPosition(): Promise<Long>

	fun getContentBufferedPosition(): Promise<Long>

	fun getPlaybackLooper(): Promise<Looper>

	fun retry(): Promise<Unit>

	fun setMediaSources(mediaSources: MutableList<MediaSource>): Promise<Unit>

	fun setMediaSources(mediaSources: MutableList<MediaSource>, resetPosition: Boolean): Promise<Unit>

	fun setMediaSources(mediaSources: MutableList<MediaSource>, startWindowIndex: Int, startPositionMs: Long): Promise<Unit>

	fun setMediaSource(mediaSource: MediaSource): Promise<Unit>

	fun setMediaSource(mediaSource: MediaSource, startPositionMs: Long): Promise<Unit>

	fun setMediaSource(mediaSource: MediaSource, resetPosition: Boolean): Promise<Unit>

	fun addMediaSource(mediaSource: MediaSource): Promise<Unit>

	fun addMediaSource(index: Int, mediaSource: MediaSource): Promise<Unit>

	fun addMediaSources(mediaSources: MutableList<MediaSource>): Promise<Unit>

	fun addMediaSources(index: Int, mediaSources: MutableList<MediaSource>): Promise<Unit>

	fun setShuffleOrder(shuffleOrder: ShuffleOrder): Promise<Unit>

	fun createMessage(target: PlayerMessage.Target): Promise<PlayerMessage>

	fun setSeekParameters(seekParameters: SeekParameters?): Promise<Unit>

	fun getSeekParameters(): Promise<SeekParameters>

	fun setForegroundMode(foregroundMode: Boolean): Promise<Unit>

	fun setPauseAtEndOfMediaItems(pauseAtEndOfMediaItems: Boolean): Promise<Unit>

	fun getPauseAtEndOfMediaItems(): Promise<Boolean>

	fun experimentalSetOffloadSchedulingEnabled(offloadSchedulingEnabled: Boolean): Promise<Unit>
}
