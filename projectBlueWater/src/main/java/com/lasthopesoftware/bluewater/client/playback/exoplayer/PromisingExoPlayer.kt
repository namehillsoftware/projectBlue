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
	fun getAudioComponent(): Promise<ExoPlayer.AudioComponent?>

	fun getVideoComponent(): Promise<ExoPlayer.VideoComponent?>

	fun getTextComponent(): Promise<ExoPlayer.TextComponent?>

	fun getMetadataComponent(): Promise<ExoPlayer.MetadataComponent?>

	fun getDeviceComponent(): Promise<ExoPlayer.DeviceComponent?>

	fun getApplicationLooper(): Promise<Looper>

	fun addListener(listener: Player.EventListener): Promise<PromisingExoPlayer>

	fun removeListener(listener: Player.EventListener): Promise<PromisingExoPlayer>

	fun setMediaItems(mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer>

	fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean): Promise<PromisingExoPlayer>

	fun setMediaItems(mediaItems: MutableList<MediaItem>, startWindowIndex: Int, startPositionMs: Long): Promise<PromisingExoPlayer>

	fun setMediaItem(mediaItem: MediaItem): Promise<PromisingExoPlayer>

	fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long): Promise<PromisingExoPlayer>

	fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean): Promise<PromisingExoPlayer>

	fun addMediaItem(mediaItem: MediaItem): Promise<PromisingExoPlayer>

	fun addMediaItem(index: Int, mediaItem: MediaItem): Promise<PromisingExoPlayer>

	fun addMediaItems(mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer>

	fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer>

	fun moveMediaItem(currentIndex: Int, newIndex: Int): Promise<PromisingExoPlayer>

	fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): Promise<PromisingExoPlayer>

	fun removeMediaItem(index: Int): Promise<PromisingExoPlayer>

	fun removeMediaItems(fromIndex: Int, toIndex: Int): Promise<PromisingExoPlayer>

	fun clearMediaItems(): Promise<PromisingExoPlayer>

	fun prepare(): Promise<PromisingExoPlayer>

	fun getPlaybackState(): Promise<Int>

	fun getPlaybackSuppressionReason(): Promise<Int>

	fun isPlaying(): Promise<Boolean>

	fun getPlayerError(): Promise<ExoPlaybackException?>

	fun play(): Promise<PromisingExoPlayer>

	fun pause(): Promise<PromisingExoPlayer>

	fun setPlayWhenReady(playWhenReady: Boolean): Promise<PromisingExoPlayer>

	fun getPlayWhenReady(): Promise<Boolean>

	fun setRepeatMode(repeatMode: Int): Promise<PromisingExoPlayer>

	fun getRepeatMode(): Promise<Int>

	fun setShuffleModeEnabled(shuffleModeEnabled: Boolean): Promise<PromisingExoPlayer>

	fun getShuffleModeEnabled(): Promise<Boolean>

	fun isLoading(): Promise<Boolean>

	fun seekToDefaultPosition(): Promise<PromisingExoPlayer>

	fun seekToDefaultPosition(windowIndex: Int): Promise<PromisingExoPlayer>

	fun seekTo(positionMs: Long): Promise<PromisingExoPlayer>

	fun seekTo(windowIndex: Int, positionMs: Long): Promise<PromisingExoPlayer>

	fun hasPreviousWindow(): Promise<Boolean>

	fun seekToPreviousWindow(): Promise<PromisingExoPlayer>

	fun hasNextWindow(): Promise<Boolean>

	fun seekToNextWindow(): Promise<PromisingExoPlayer>

	fun setPlaybackParameters(playbackParameters: PlaybackParameters): Promise<PromisingExoPlayer>

	fun getPlaybackParameters(): Promise<PlaybackParameters>

	fun stop(): Promise<PromisingExoPlayer>

	fun release(): Promise<PromisingExoPlayer>

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

	fun setMediaSources(mediaSources: MutableList<MediaSource>): Promise<PromisingExoPlayer>

	fun setMediaSources(mediaSources: MutableList<MediaSource>, resetPosition: Boolean): Promise<PromisingExoPlayer>

	fun setMediaSources(mediaSources: MutableList<MediaSource>, startWindowIndex: Int, startPositionMs: Long): Promise<PromisingExoPlayer>

	fun setMediaSource(mediaSource: MediaSource): Promise<PromisingExoPlayer>

	fun setMediaSource(mediaSource: MediaSource, startPositionMs: Long): Promise<PromisingExoPlayer>

	fun setMediaSource(mediaSource: MediaSource, resetPosition: Boolean): Promise<PromisingExoPlayer>

	fun addMediaSource(mediaSource: MediaSource): Promise<PromisingExoPlayer>

	fun addMediaSource(index: Int, mediaSource: MediaSource): Promise<PromisingExoPlayer>

	fun addMediaSources(mediaSources: MutableList<MediaSource>): Promise<PromisingExoPlayer>

	fun addMediaSources(index: Int, mediaSources: MutableList<MediaSource>): Promise<PromisingExoPlayer>

	fun setShuffleOrder(shuffleOrder: ShuffleOrder): Promise<PromisingExoPlayer>

	fun createMessage(target: PlayerMessage.Target): Promise<PlayerMessage>

	fun setSeekParameters(seekParameters: SeekParameters?): Promise<PromisingExoPlayer>

	fun getSeekParameters(): Promise<SeekParameters>

	fun setForegroundMode(foregroundMode: Boolean): Promise<PromisingExoPlayer>

	fun setPauseAtEndOfMediaItems(pauseAtEndOfMediaItems: Boolean): Promise<PromisingExoPlayer>

	fun getPauseAtEndOfMediaItems(): Promise<Boolean>

	fun experimentalSetOffloadSchedulingEnabled(offloadSchedulingEnabled: Boolean): Promise<PromisingExoPlayer>
}
