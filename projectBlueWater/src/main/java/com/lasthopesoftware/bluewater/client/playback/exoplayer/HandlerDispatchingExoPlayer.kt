package com.lasthopesoftware.bluewater.client.playback.exoplayer

import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.exoplayer.trackselection.TrackSelector
import com.lasthopesoftware.promises.extensions.LoopedInPromise.Companion.loopIn
import com.namehillsoftware.handoff.promises.Promise

class HandlerDispatchingExoPlayer(private val innerPlayer: ExoPlayer, private val handler: Handler) :
	PromisingExoPlayer {

	override fun getApplicationLooper(): Promise<Looper> = handler.loopIn { innerPlayer.applicationLooper }

	override fun addListener(listener: Player.Listener): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.addListener(listener)
			this
		}

	override fun removeListener(listener: Player.Listener): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.removeListener(listener)
			this
		}

	override fun setMediaItems(mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setMediaItems(mediaItems)
			this
		}

	override fun setMediaItems(
		mediaItems: MutableList<MediaItem>,
		resetPosition: Boolean): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setMediaItems(mediaItems, resetPosition)
			this
		}

	override fun setMediaItems(
		mediaItems: MutableList<MediaItem>,
		startWindowIndex: Int,
		startPositionMs: Long
	): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setMediaItems(mediaItems, startWindowIndex, startPositionMs)
			this
		}

	override fun setMediaItem(mediaItem: MediaItem): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setMediaItem(mediaItem)
			this
		}

	override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setMediaItem(mediaItem, startPositionMs)
			this
		}

	override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setMediaItem(mediaItem, resetPosition)
			this
		}

	override fun setVolume(volume: Float): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.volume = volume
			this
		}

	override fun addMediaItem(mediaItem: MediaItem): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.addMediaItem(mediaItem)
			this
		}

	override fun addMediaItem(index: Int, mediaItem: MediaItem): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.addMediaItem(index, mediaItem)
			this
		}

	override fun addMediaItems(mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.addMediaItems(mediaItems)
			this
		}

	override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.addMediaItems(index, mediaItems)
			this
		}

	override fun moveMediaItem(currentIndex: Int, newIndex: Int): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.moveMediaItem(currentIndex, newIndex)
			this
		}

	override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.moveMediaItems(fromIndex, toIndex, newIndex)
			this
		}

	override fun removeMediaItem(index: Int): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.removeMediaItem(index)
			this
		}

	override fun removeMediaItems(fromIndex: Int, toIndex: Int): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.removeMediaItems(fromIndex, toIndex)
			this
		}

	override fun clearMediaItems(): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.clearMediaItems()
			this
		}

	override fun prepare(): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.prepare()
			this
		}

	override fun getPlaybackState(): Promise<Int> =
		handler.loopIn { innerPlayer.playbackState }

	override fun getPlaybackSuppressionReason(): Promise<Int> =
		handler.loopIn { innerPlayer.playbackSuppressionReason }

	override fun isPlaying(): Promise<Boolean> =
		handler.loopIn { innerPlayer.isPlaying }

	override fun getPlayerError(): Promise<ExoPlaybackException?> =
		handler.loopIn { innerPlayer.playerError }

	override fun play(): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.play()
			this
		}

	override fun pause(): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.pause()
			this
		}

	override fun setPlayWhenReady(playWhenReady: Boolean): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.playWhenReady = playWhenReady
			this
		}

	override fun getPlayWhenReady(): Promise<Boolean> =
		handler.loopIn { innerPlayer.playWhenReady }

	override fun setRepeatMode(repeatMode: Int): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.repeatMode = repeatMode
			this
		}

	override fun getRepeatMode(): Promise<Int> =
		handler.loopIn { innerPlayer.repeatMode }

	override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.shuffleModeEnabled = shuffleModeEnabled
			this
		}

	override fun getShuffleModeEnabled(): Promise<Boolean> =
		handler.loopIn { innerPlayer.shuffleModeEnabled }

	override fun isLoading(): Promise<Boolean> =
		handler.loopIn { innerPlayer.isLoading }

	override fun seekToDefaultPosition(): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.seekToDefaultPosition()
			this
		}

	override fun seekToDefaultPosition(windowIndex: Int): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.seekToDefaultPosition(windowIndex)
			this
		}

	override fun seekTo(positionMs: Long): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.seekTo(positionMs)
			this
		}

	override fun seekTo(windowIndex: Int, positionMs: Long): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.seekTo(windowIndex, positionMs)
			this
		}

	override fun hasPreviousMediaItem(): Promise<Boolean> =
		handler.loopIn { innerPlayer.hasPreviousMediaItem() }

	override fun seekToPreviousMediaItem(): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.seekToPreviousMediaItem()
			this
		}

	override fun hasNextMediaItem(): Promise<Boolean> =
		handler.loopIn { innerPlayer.hasNextMediaItem() }

	override fun seekToNextMediaItem(): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.seekToNextMediaItem()
			this
		}

	override fun setPlaybackParameters(playbackParameters: PlaybackParameters): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.playbackParameters = playbackParameters
			this
		}

	override fun getPlaybackParameters(): Promise<PlaybackParameters> =
		handler.loopIn { innerPlayer.playbackParameters }

	override fun stop(): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.stop()
			this
		}

	override fun release(): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.release()
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getRendererCount(): Promise<Int> =
		handler.loopIn { innerPlayer.rendererCount }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getRendererType(index: Int): Promise<Int> =
		handler.loopIn { innerPlayer.getRendererType(index) }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getTrackSelector(): Promise<TrackSelector?> =
		handler.loopIn { innerPlayer.trackSelector }

	override fun getCurrentTracks(): Promise<Tracks?> =
		handler.loopIn { innerPlayer.currentTracks }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getCurrentManifest(): Promise<Any?> =
		handler.loopIn { innerPlayer.currentManifest }

	override fun getCurrentTimeline(): Promise<Timeline> =
		handler.loopIn { innerPlayer.currentTimeline }

	override fun getCurrentPeriodIndex(): Promise<Int> =
		handler.loopIn { innerPlayer.currentPeriodIndex }

	override fun getCurrentMediaItemIndex(): Promise<Int> =
		handler.loopIn { innerPlayer.currentMediaItemIndex }

	override fun getNextMediaItemIndex(): Promise<Int> =
		handler.loopIn { innerPlayer.nextMediaItemIndex }

	override fun getPreviousMediaItemIndex(): Promise<Int> =
		handler.loopIn { innerPlayer.previousMediaItemIndex }

	override fun getCurrentMediaItem(): Promise<MediaItem?> =
		handler.loopIn { innerPlayer.currentMediaItem }

	override fun getMediaItemCount(): Promise<Int> =
		handler.loopIn { innerPlayer.mediaItemCount }

	override fun getMediaItemAt(index: Int): Promise<MediaItem> =
		handler.loopIn { innerPlayer.getMediaItemAt(index) }

	override fun getDuration(): Promise<Long> =
		handler.loopIn { innerPlayer.duration }

	override fun getCurrentPosition(): Promise<Long> =
		handler.loopIn { innerPlayer.currentPosition }

	override fun getBufferedPosition(): Promise<Long> =
		handler.loopIn { innerPlayer.bufferedPosition }

	override fun getBufferedPercentage(): Promise<Int> =
		handler.loopIn { innerPlayer.bufferedPercentage }

	override fun getTotalBufferedDuration(): Promise<Long> =
		handler.loopIn { innerPlayer.totalBufferedDuration }

	override fun isCurrentMediaItemDynamic(): Promise<Boolean> =
		handler.loopIn { innerPlayer.isCurrentMediaItemDynamic }

	override fun isCurrentMediaItemLive(): Promise<Boolean> =
		handler.loopIn { innerPlayer.isCurrentMediaItemLive }

	override fun getCurrentLiveOffset(): Promise<Long> =
		handler.loopIn { innerPlayer.currentLiveOffset }

	override fun isCurrentMediaItemSeekable(): Promise<Boolean> =
		handler.loopIn { innerPlayer.isCurrentMediaItemSeekable }

	override fun isPlayingAd(): Promise<Boolean> =
		handler.loopIn { innerPlayer.isPlayingAd }

	override fun getCurrentAdGroupIndex(): Promise<Int> =
		handler.loopIn { innerPlayer.currentAdGroupIndex }

	override fun getCurrentAdIndexInAdGroup(): Promise<Int> =
		handler.loopIn { innerPlayer.currentAdIndexInAdGroup }

	override fun getContentDuration(): Promise<Long> =
		handler.loopIn { innerPlayer.contentDuration }

	override fun getContentPosition(): Promise<Long> =
		handler.loopIn { innerPlayer.contentPosition }

	override fun getContentBufferedPosition(): Promise<Long> =
		handler.loopIn { innerPlayer.contentBufferedPosition }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getPlaybackLooper(): Promise<Looper> =
		handler.loopIn { innerPlayer.playbackLooper }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setMediaSources(mediaSources: MutableList<MediaSource>): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setMediaSources(mediaSources)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setMediaSources(
		mediaSources: MutableList<MediaSource>,
		resetPosition: Boolean
	): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setMediaSources(mediaSources, resetPosition)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setMediaSources(
		mediaSources: MutableList<MediaSource>,
		startWindowIndex: Int,
		startPositionMs: Long
	): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setMediaSources(mediaSources, startWindowIndex, startPositionMs)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setMediaSource(mediaSource: MediaSource): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setMediaSource(mediaSource)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setMediaSource(mediaSource: MediaSource, startPositionMs: Long): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setMediaSource(mediaSource, startPositionMs)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setMediaSource(mediaSource: MediaSource, resetPosition: Boolean): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setMediaSource(mediaSource, resetPosition)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun addMediaSource(mediaSource: MediaSource): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.addMediaSource(mediaSource)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun addMediaSource(index: Int, mediaSource: MediaSource): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.addMediaSource(index, mediaSource)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun addMediaSources(mediaSources: MutableList<MediaSource>): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.addMediaSources(mediaSources)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun addMediaSources(index: Int, mediaSources: MutableList<MediaSource>): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.addMediaSources(index, mediaSources)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setShuffleOrder(shuffleOrder: ShuffleOrder): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setShuffleOrder(shuffleOrder)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun createMessage(target: PlayerMessage.Target): Promise<PlayerMessage> =
		handler.loopIn { innerPlayer.createMessage(target) }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setSeekParameters(seekParameters: SeekParameters?): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setSeekParameters(seekParameters)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getSeekParameters(): Promise<SeekParameters> =
		handler.loopIn { innerPlayer.seekParameters }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setForegroundMode(foregroundMode: Boolean): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.setForegroundMode(foregroundMode)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setPauseAtEndOfMediaItems(pauseAtEndOfMediaItems: Boolean): Promise<PromisingExoPlayer> =
		handler.loopIn {
			innerPlayer.pauseAtEndOfMediaItems = pauseAtEndOfMediaItems
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getPauseAtEndOfMediaItems(): Promise<Boolean> =
		handler.loopIn { innerPlayer.pauseAtEndOfMediaItems }
}
