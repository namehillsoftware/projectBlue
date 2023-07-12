package com.lasthopesoftware.bluewater.client.playback.exoplayer

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.PlayerMessage
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.exoplayer.trackselection.TrackSelector
import com.namehillsoftware.handoff.promises.Promise

interface PromisingExoPlayer {

	fun getApplicationLooper(): Promise<Looper>

	fun addListener(listener: Player.Listener): Promise<PromisingExoPlayer>

	fun removeListener(listener: Player.Listener): Promise<PromisingExoPlayer>

	fun setMediaItems(mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer>

	fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean): Promise<PromisingExoPlayer>

	fun setMediaItems(mediaItems: MutableList<MediaItem>, startWindowIndex: Int, startPositionMs: Long): Promise<PromisingExoPlayer>

	fun setMediaItem(mediaItem: MediaItem): Promise<PromisingExoPlayer>

	fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long): Promise<PromisingExoPlayer>

	fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean): Promise<PromisingExoPlayer>

	fun setVolume(volume: Float): Promise<PromisingExoPlayer>

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

	fun hasPreviousMediaItem(): Promise<Boolean>

	fun seekToPreviousMediaItem(): Promise<PromisingExoPlayer>

	fun hasNextMediaItem(): Promise<Boolean>

	fun seekToNextMediaItem(): Promise<PromisingExoPlayer>

	fun setPlaybackParameters(playbackParameters: PlaybackParameters): Promise<PromisingExoPlayer>

	fun getPlaybackParameters(): Promise<PlaybackParameters>

	fun stop(): Promise<PromisingExoPlayer>

	fun release(): Promise<PromisingExoPlayer>

	fun getRendererCount(): Promise<Int>

	fun getRendererType(index: Int): Promise<Int>

	fun getTrackSelector(): Promise<TrackSelector?>

	fun getCurrentTracks(): Promise<Tracks?>

	fun getCurrentManifest(): Promise<Any?>

	fun getCurrentTimeline(): Promise<Timeline>

	fun getCurrentPeriodIndex(): Promise<Int>

	fun getCurrentMediaItemIndex(): Promise<Int>

	fun getNextMediaItemIndex(): Promise<Int>

	fun getPreviousMediaItemIndex(): Promise<Int>

	fun getCurrentMediaItem(): Promise<MediaItem?>

	fun getMediaItemCount(): Promise<Int>

	fun getMediaItemAt(index: Int): Promise<MediaItem>

	fun getDuration(): Promise<Long>

	fun getCurrentPosition(): Promise<Long>

	fun getBufferedPosition(): Promise<Long>

	fun getBufferedPercentage(): Promise<Int>

	fun getTotalBufferedDuration(): Promise<Long>

	fun isCurrentMediaItemDynamic(): Promise<Boolean>

	fun isCurrentMediaItemLive(): Promise<Boolean>

	fun getCurrentLiveOffset(): Promise<Long>

	fun isCurrentMediaItemSeekable(): Promise<Boolean>

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
