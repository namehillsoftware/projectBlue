package com.lasthopesoftware.bluewater.client.playback.exoplayer

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
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.resources.executors.HandlerExecutor
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter

class HandlerDispatchingExoPlayer(private val innerPlayer: ExoPlayer, private val executor: HandlerExecutor)
	: PromisingExoPlayer
{
	override fun addListener(listener: Player.Listener): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.addListener(listener)
			this
		}

	override fun removeListener(listener: Player.Listener): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.removeListener(listener)
			this
		}

	override fun setMediaItems(mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setMediaItems(mediaItems)
			this
		}

	override fun setMediaItems(
		mediaItems: MutableList<MediaItem>,
		resetPosition: Boolean): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setMediaItems(mediaItems, resetPosition)
			this
		}

	override fun setMediaItems(
		mediaItems: MutableList<MediaItem>,
		startWindowIndex: Int,
		startPositionMs: Long
	): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setMediaItems(mediaItems, startWindowIndex, startPositionMs)
			this
		}

	override fun setMediaItem(mediaItem: MediaItem): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setMediaItem(mediaItem)
			this
		}

	override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setMediaItem(mediaItem, startPositionMs)
			this
		}

	override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setMediaItem(mediaItem, resetPosition)
			this
		}

	override fun setVolume(volume: Float): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.volume = volume
			this
		}

	override fun addMediaItem(mediaItem: MediaItem): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.addMediaItem(mediaItem)
			this
		}

	override fun addMediaItem(index: Int, mediaItem: MediaItem): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.addMediaItem(index, mediaItem)
			this
		}

	override fun addMediaItems(mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.addMediaItems(mediaItems)
			this
		}

	override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.addMediaItems(index, mediaItems)
			this
		}

	override fun moveMediaItem(currentIndex: Int, newIndex: Int): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.moveMediaItem(currentIndex, newIndex)
			this
		}

	override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.moveMediaItems(fromIndex, toIndex, newIndex)
			this
		}

	override fun removeMediaItem(index: Int): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.removeMediaItem(index)
			this
		}

	override fun removeMediaItems(fromIndex: Int, toIndex: Int): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.removeMediaItems(fromIndex, toIndex)
			this
		}

	override fun clearMediaItems(): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.clearMediaItems()
			this
		}

	override fun prepare(): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.prepare()
			this
		}

	override fun getPlaybackState(): Promise<Int> =
		loopIn { innerPlayer.playbackState }

	override fun getPlaybackSuppressionReason(): Promise<Int> =
		loopIn { innerPlayer.playbackSuppressionReason }

	override fun isPlaying(): Promise<Boolean> =
		loopIn { innerPlayer.isPlaying }

	override fun getPlayerError(): Promise<ExoPlaybackException?> =
		loopIn { innerPlayer.playerError }

	override fun play(): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.play()
			this
		}

	override fun pause(): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.pause()
			this
		}

	override fun setPlayWhenReady(playWhenReady: Boolean): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.playWhenReady = playWhenReady
			this
		}

	override fun getPlayWhenReady(): Promise<Boolean> =
		loopIn { innerPlayer.playWhenReady }

	override fun setRepeatMode(repeatMode: Int): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.repeatMode = repeatMode
			this
		}

	override fun getRepeatMode(): Promise<Int> =
		loopIn { innerPlayer.repeatMode }

	override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.shuffleModeEnabled = shuffleModeEnabled
			this
		}

	override fun getShuffleModeEnabled(): Promise<Boolean> =
		loopIn { innerPlayer.shuffleModeEnabled }

	override fun isLoading(): Promise<Boolean> =
		loopIn { innerPlayer.isLoading }

	override fun seekToDefaultPosition(): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.seekToDefaultPosition()
			this
		}

	override fun seekToDefaultPosition(windowIndex: Int): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.seekToDefaultPosition(windowIndex)
			this
		}

	override fun seekTo(positionMs: Long): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.seekTo(positionMs)
			this
		}

	override fun seekTo(windowIndex: Int, positionMs: Long): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.seekTo(windowIndex, positionMs)
			this
		}

	override fun hasPreviousMediaItem(): Promise<Boolean> =
		loopIn { innerPlayer.hasPreviousMediaItem() }

	override fun seekToPreviousMediaItem(): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.seekToPreviousMediaItem()
			this
		}

	override fun hasNextMediaItem(): Promise<Boolean> =
		loopIn { innerPlayer.hasNextMediaItem() }

	override fun seekToNextMediaItem(): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.seekToNextMediaItem()
			this
		}

	override fun setPlaybackParameters(playbackParameters: PlaybackParameters): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.playbackParameters = playbackParameters
			this
		}

	override fun getPlaybackParameters(): Promise<PlaybackParameters> =
		loopIn { innerPlayer.playbackParameters }

	override fun stop(): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.stop()
			this
		}

	override fun release(): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.release()
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getRendererCount(): Promise<Int> =
		loopIn { innerPlayer.rendererCount }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getRendererType(index: Int): Promise<Int> =
		loopIn { innerPlayer.getRendererType(index) }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getTrackSelector(): Promise<TrackSelector?> =
		loopIn { innerPlayer.trackSelector }

	override fun getCurrentTracks(): Promise<Tracks?> =
		loopIn { innerPlayer.currentTracks }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getCurrentManifest(): Promise<Any?> =
		loopIn { innerPlayer.currentManifest }

	override fun getCurrentTimeline(): Promise<Timeline> =
		loopIn { innerPlayer.currentTimeline }

	override fun getCurrentPeriodIndex(): Promise<Int> =
		loopIn { innerPlayer.currentPeriodIndex }

	override fun getCurrentMediaItemIndex(): Promise<Int> =
		loopIn { innerPlayer.currentMediaItemIndex }

	override fun getNextMediaItemIndex(): Promise<Int> =
		loopIn { innerPlayer.nextMediaItemIndex }

	override fun getPreviousMediaItemIndex(): Promise<Int> =
		loopIn { innerPlayer.previousMediaItemIndex }

	override fun getCurrentMediaItem(): Promise<MediaItem?> =
		loopIn { innerPlayer.currentMediaItem }

	override fun getMediaItemCount(): Promise<Int> =
		loopIn { innerPlayer.mediaItemCount }

	override fun getMediaItemAt(index: Int): Promise<MediaItem> =
		loopIn { innerPlayer.getMediaItemAt(index) }

	override fun getDuration(): Promise<Long> =
		loopIn { innerPlayer.duration }

	override fun getCurrentPosition(): Promise<Long> =
		loopIn { innerPlayer.currentPosition }

	override fun getBufferedPosition(): Promise<Long> =
		loopIn { innerPlayer.bufferedPosition }

	override fun getBufferedPercentage(): Promise<Int> =
		loopIn { innerPlayer.bufferedPercentage }

	override fun getTotalBufferedDuration(): Promise<Long> =
		loopIn { innerPlayer.totalBufferedDuration }

	override fun isCurrentMediaItemDynamic(): Promise<Boolean> =
		loopIn { innerPlayer.isCurrentMediaItemDynamic }

	override fun isCurrentMediaItemLive(): Promise<Boolean> =
		loopIn { innerPlayer.isCurrentMediaItemLive }

	override fun getCurrentLiveOffset(): Promise<Long> =
		loopIn { innerPlayer.currentLiveOffset }

	override fun isCurrentMediaItemSeekable(): Promise<Boolean> =
		loopIn { innerPlayer.isCurrentMediaItemSeekable }

	override fun isPlayingAd(): Promise<Boolean> =
		loopIn { innerPlayer.isPlayingAd }

	override fun getCurrentAdGroupIndex(): Promise<Int> =
		loopIn { innerPlayer.currentAdGroupIndex }

	override fun getCurrentAdIndexInAdGroup(): Promise<Int> =
		loopIn { innerPlayer.currentAdIndexInAdGroup }

	override fun getContentDuration(): Promise<Long> =
		loopIn { innerPlayer.contentDuration }

	override fun getContentPosition(): Promise<Long> =
		loopIn { innerPlayer.contentPosition }

	override fun getContentBufferedPosition(): Promise<Long> =
		loopIn { innerPlayer.contentBufferedPosition }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getPlaybackLooper(): Promise<Looper> =
		loopIn { innerPlayer.playbackLooper }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setMediaSources(mediaSources: MutableList<MediaSource>): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setMediaSources(mediaSources)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setMediaSources(
		mediaSources: MutableList<MediaSource>,
		resetPosition: Boolean
	): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setMediaSources(mediaSources, resetPosition)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setMediaSources(
		mediaSources: MutableList<MediaSource>,
		startWindowIndex: Int,
		startPositionMs: Long
	): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setMediaSources(mediaSources, startWindowIndex, startPositionMs)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setMediaSource(mediaSource: MediaSource): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setMediaSource(mediaSource)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setMediaSource(mediaSource: MediaSource, startPositionMs: Long): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setMediaSource(mediaSource, startPositionMs)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setMediaSource(mediaSource: MediaSource, resetPosition: Boolean): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setMediaSource(mediaSource, resetPosition)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun addMediaSource(mediaSource: MediaSource): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.addMediaSource(mediaSource)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun addMediaSource(index: Int, mediaSource: MediaSource): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.addMediaSource(index, mediaSource)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun addMediaSources(mediaSources: MutableList<MediaSource>): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.addMediaSources(mediaSources)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun addMediaSources(index: Int, mediaSources: MutableList<MediaSource>): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.addMediaSources(index, mediaSources)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setShuffleOrder(shuffleOrder: ShuffleOrder): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setShuffleOrder(shuffleOrder)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun createMessage(target: PlayerMessage.Target): Promise<PlayerMessage> =
		loopIn { innerPlayer.createMessage(target) }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setSeekParameters(seekParameters: SeekParameters?): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setSeekParameters(seekParameters)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getSeekParameters(): Promise<SeekParameters> =
		loopIn { innerPlayer.seekParameters }

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setForegroundMode(foregroundMode: Boolean): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.setForegroundMode(foregroundMode)
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun setPauseAtEndOfMediaItems(pauseAtEndOfMediaItems: Boolean): Promise<PromisingExoPlayer> =
		loopIn {
			innerPlayer.pauseAtEndOfMediaItems = pauseAtEndOfMediaItems
			this
		}

	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	override fun getPauseAtEndOfMediaItems(): Promise<Boolean> =
		loopIn { innerPlayer.pauseAtEndOfMediaItems }

	private fun <Response> loopIn(messageWriter: CancellableMessageWriter<Response>): Promise<Response> =
		executor.preparePromise(messageWriter)
}
